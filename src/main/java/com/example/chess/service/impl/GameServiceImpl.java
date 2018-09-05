package com.example.chess.service.impl;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.PointDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.ArrangementDTO;
import com.example.chess.entity.Game;
import com.example.chess.entity.History;
import com.example.chess.entity.Piece;
import com.example.chess.enums.GameMode;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.exceptions.HistoryNotFoundException;
import com.example.chess.repository.GameRepository;
import com.example.chess.repository.HistoryRepository;
import com.example.chess.repository.PieceRepository;
import com.example.chess.service.GameService;
import com.example.chess.service.MoveService;
import com.example.chess.utils.ChessUtils;
import com.example.chess.utils.MoveResult;
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.example.chess.ChessConstants.BOARD_SIZE;
import static com.example.chess.ChessConstants.ROOK_LONG_COLUMN_INDEX;
import static com.example.chess.ChessConstants.ROOK_SHORT_COLUMN_INDEX;
import static java.util.function.Function.identity;

@Log4j2
@Service
public class GameServiceImpl implements GameService {

	private final Supplier<MoveService> moveService;
	private final GameRepository gameRepository;
	private final HistoryRepository historyRepository;
	private final PieceRepository pieceRepository;

	private Map<Side, Map<PieceType, Piece>> piecesBySideAndTypeMap;

	@Value("${dev.move.mirror.enable}")
	private Boolean isMirrorEnabled;
	@Value("${app.game.bots.move-delay}")
	private Long botMoveDelay;

	@Autowired
	public GameServiceImpl(GameRepository gameRepository, HistoryRepository historyRepository, Supplier<MoveService> moveService, PieceRepository pieceRepository) {
		this.gameRepository = gameRepository;
		this.historyRepository = historyRepository;
		this.moveService = moveService;
		this.pieceRepository = pieceRepository;
	}

	@PostConstruct
	private void init() {
		Iterable<Piece> pieces = pieceRepository.findAll();
		if (Iterables.isEmpty(pieces)) {
			throw new RuntimeException("pieces not found");
		}

		piecesBySideAndTypeMap = StreamSupport
				.stream(pieces.spliterator(), false)
				.collect(Collectors.groupingBy(Piece::getSide,
						Collectors.toMap(Piece::getType, identity())));
	}

	@Override
	public Game findAndCheckGame(long gameId) throws GameNotFoundException {
		return gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
	}

	@Override
	public Set<PointDTO> getAvailableMoves(long gameId, PointDTO point) throws GameNotFoundException, HistoryNotFoundException {
		Game game = findAndCheckGame(gameId);
		List<List<CellDTO>> cellsMatrix = createCellsMatrixByGame(game);

		return getMoveServiceInstance(game, cellsMatrix).getAvailableMoves(point);
	}

	@Override
	@Transactional
	@Profile
	public ArrangementDTO applyMove(long gameId, MoveDTO move) throws HistoryNotFoundException, GameNotFoundException {
		Game game = findAndCheckGame(gameId);
		int newPosition = game.getPosition() + 1;

		List<History> beforeMoveHistory;
		if (game.getPosition() == 0) {
			beforeMoveHistory = createStartHistory(gameId);
		} else {
			beforeMoveHistory = findHistoryByGameIdAndPosition(gameId, game.getPosition());
		}

		List<List<CellDTO>> cellsMatrix = ChessUtils.createCellsMatrixByHistory(beforeMoveHistory);

		//move piece
		Piece transformationPiece = getPawnTransformationPiece(cellsMatrix, move);
		MoveResult moveResult = ChessUtils.executeMove(cellsMatrix, move, transformationPiece);
		Piece pieceFrom = moveResult.getPieceFrom();
		Side sideFrom = pieceFrom.getSide();

		game.setPawnLongMoveColumnIndex(sideFrom, null);
		game.setUnderCheckSide(null);

		if (pieceFrom.getType() == PieceType.KING) {
			//do castling (only the ROOK moves)
			checkAndExecuteCastling(cellsMatrix, move);

			game.disableShortCasting(sideFrom);
			game.disableLongCasting(sideFrom);

		} else if (pieceFrom.getType() == PieceType.ROOK) {

			if (game.isShortCastlingAvailable(sideFrom) && move.getFrom().getColumnIndex() == ROOK_SHORT_COLUMN_INDEX) {
				game.disableShortCasting(sideFrom);

			} else if (game.isLongCastlingAvailable(sideFrom) && move.getFrom().getColumnIndex() == ROOK_LONG_COLUMN_INDEX) {
				game.disableLongCasting(sideFrom);
			}
		} else if (pieceFrom.getType() == PieceType.PAWN) {
			int diff = move.getFrom().getRowIndex() - move.getTo().getRowIndex();

			if (Math.abs(diff) == 2) {//is long move
				game.setPawnLongMoveColumnIndex(sideFrom, move.getFrom().getColumnIndex());
			}

			if (!Objects.equals(move.getFrom().getColumnIndex(), move.getTo().getColumnIndex())) {

				if (moveResult.getPieceTo() == null) {
					//так это взятие на проходе (не могла же пешка покинуть свою вертикаль и при этом ничего не срубив)

					//рубим пешку
					CellDTO enemyPawnCell = ChessUtils.getCell(cellsMatrix, move.getFrom().getRowIndex(), move.getTo().getColumnIndex());
					enemyPawnCell.setPiece(null);
				}
			}
		}

		List<History> afterMoveHistory = createHistoryByCellsMatrix(cellsMatrix, gameId, newPosition);
		game.setPosition(newPosition);

		boolean isEnemyKingUnderAttack = getMoveServiceInstance(game, cellsMatrix).isEnemyKingUnderAttack(sideFrom);
		if (isEnemyKingUnderAttack) {
			game.setUnderCheckSide(sideFrom.reverse());
		}

		game.getSideFeatures(sideFrom).setLastVisitDate(LocalDateTime.now());

		historyRepository.saveAll(afterMoveHistory);
		gameRepository.save(game);

		return new ArrangementDTO(newPosition, cellsMatrix, game.getUnderCheckSide());
	}

	private void checkAndExecuteCastling(List<List<CellDTO>> cellsMatrix, MoveDTO move) {
		int diff = move.getFrom().getColumnIndex() - move.getTo().getColumnIndex();

		if (Math.abs(diff) == 2) {    //is castling
			Integer kingFromColumnIndex = move.getFrom().getColumnIndex();

			//short
			PointDTO rookFrom = new PointDTO(move.getFrom().getRowIndex(), ROOK_SHORT_COLUMN_INDEX);
			PointDTO rookTo = new PointDTO(move.getFrom().getRowIndex(), kingFromColumnIndex - 1);

			//long
			if (diff < 0) {
				rookFrom.setColumnIndex(ROOK_LONG_COLUMN_INDEX);
				rookTo.setColumnIndex(kingFromColumnIndex + 1);
			}

			//move ROOK
			ChessUtils.executeMove(cellsMatrix, new MoveDTO(rookFrom, rookTo));
		}
	}

	private Piece getPawnTransformationPiece(List<List<CellDTO>> cellsMatrix, MoveDTO move) {
		if (move.getPieceType() == null) {
			return null;
		}

		CellDTO cellFrom = ChessUtils.getCell(cellsMatrix, move.getFrom());
		return findPieceBySideAndType(cellFrom.getPieceSide(), move.getPieceType());
	}

	@Override
	public ArrangementDTO getArrangementByPosition(Game game, int position) throws HistoryNotFoundException {
		ArrangementDTO result = new ArrangementDTO();

		if (position > 0) {
			result.setCellsMatrix(createCellsMatrixByGameIdAndPosition(game.getId(), position));
		} else {
			result.setCellsMatrix(createStartCellsMatrix(game.getId()));
		}

		result.setPosition(position);
		result.setUnderCheckSide(game.getUnderCheckSide());

		return result;
	}

	@Override
	public ArrangementDTO getArrangementByPosition(long gameId, int position) throws HistoryNotFoundException, GameNotFoundException {
		Game game = findAndCheckGame(gameId);
		return getArrangementByPosition(game, position);
	}

	private List<List<CellDTO>> createCellsMatrixByGame(Game game) throws HistoryNotFoundException {
		return createCellsMatrixByGameIdAndPosition(game.getId(), game.getPosition());
	}

	private List<List<CellDTO>> createCellsMatrixByGameIdAndPosition(long gameId, int position) throws HistoryNotFoundException {
		if (position == 0) {
			return createStartCellsMatrix(gameId);
		}

		List<History> historyList = findHistoryByGameIdAndPosition(gameId, position);
		return ChessUtils.createCellsMatrixByHistory(historyList);
	}

	private List<History> findHistoryByGameIdAndPosition(long gameId, int position) throws HistoryNotFoundException {
		List<History> historyList = historyRepository.findByGameIdAndPositionOrderByRowIndexAscColumnIndexAsc(gameId, position);
		if (historyList.isEmpty()) {
			throw new HistoryNotFoundException();
		}
		return historyList;
	}

	private List<History> createHistoryByCellsMatrix(List<List<CellDTO>> cellsMatrix, Long gameId, int position) {
		return cellsMatrix.stream()
				.flatMap(List::stream)                        //convert matrix (List<List<T>> x8x8) to simple list (List<T> x64)
				.filter(cell -> cell.getPiece() != null)
				.map(cell -> History.createByCell(cell, gameId, position))
				.collect(Collectors.toList());
	}

	private List<History> createStartHistory(long gameId) {
		List<History> historyList = new ArrayList<>();

		//1-8
		for (int rowIndex = 0; rowIndex < BOARD_SIZE; rowIndex++) {
			//A-H
			for (int columnIndex = 0; columnIndex < BOARD_SIZE; columnIndex++) {

				Side side = null;
				if (rowIndex == 0 || rowIndex == 1) {
					side = Side.WHITE;
				} else if (rowIndex == 7 || rowIndex == 6) {
					side = Side.BLACK;
				}

				PieceType pieceType = null;
				if (rowIndex == 1 || rowIndex == 6) {
					pieceType = PieceType.PAWN;
				} else if (rowIndex == 0 || rowIndex == 7) {
					if (columnIndex == 0 || columnIndex == 7) {
						pieceType = PieceType.ROOK;
					} else if (columnIndex == 1 || columnIndex == 6) {
						pieceType = PieceType.KNIGHT;
					} else if (columnIndex == 2 || columnIndex == 5) {
						pieceType = PieceType.BISHOP;
					} else if (columnIndex == 3) {
						pieceType = PieceType.KING;
					} else {  //columnIndex == 4
						pieceType = PieceType.QUEEN;
					}
				}

				if (side != null) {
					Piece piece = findPieceBySideAndType(side, pieceType);

					History item = new History();
					item.setGameId(gameId);
					item.setPosition(0);
					item.setPieceId(piece.getId());
					item.setPiece(piece);
					item.setRowIndex(rowIndex);
					item.setColumnIndex(columnIndex);

					historyList.add(item);
				}
			}

		}

		return historyList;
	}

	private List<List<CellDTO>> createStartCellsMatrix(long gameId) {
		return ChessUtils.createCellsMatrixByHistory(createStartHistory(gameId));
	}

	private Piece findPieceBySideAndType(Side side, PieceType type) {
		return piecesBySideAndTypeMap.get(side).get(type);
	}

	private MoveService getMoveServiceInstance(Game game, List<List<CellDTO>> cellsMatrix) {
		MoveService moveServiceInstance = moveService.get();
		moveServiceInstance.setGame(game);
		moveServiceInstance.setCellsMatrix(cellsMatrix);
		return moveServiceInstance;
	}

	@Override
	public void applyMirrorMove(long gameId, MoveDTO dto) throws GameNotFoundException {
		if (isMirrorEnabled) {
			Game game = findAndCheckGame(gameId);

			if (game.getMode() == GameMode.AI) {
				executeInSecondaryThread(() -> {
					Thread.sleep(botMoveDelay);
					MoveDTO mirrorMoveDTO = dto.getMirror();
					applyMove(gameId, mirrorMoveDTO);
				});
			}
		}
	}

	@Override
	public void applyFirstBotMove(long gameId) throws GameNotFoundException {
		if (isMirrorEnabled) {
			Game game = findAndCheckGame(gameId);

			Side selectedSide = null;
			if (game.getWhiteFeatures().getSessionId() != null && game.getBlackFeatures().getSessionId() == null) {
				selectedSide = Side.WHITE;
			}
			if (game.getBlackFeatures().getSessionId() != null && game.getWhiteFeatures().getSessionId() == null) {
				selectedSide = Side.BLACK;
			}

			if (game.getMode() == GameMode.AI && selectedSide == Side.BLACK) {
				executeInSecondaryThread(() -> {
					Thread.sleep(botMoveDelay);
					MoveDTO moveDTO = new MoveDTO();
					moveDTO.setFrom(new PointDTO(1, PieceType.KING.getStartColumnIndex()));
					moveDTO.setTo(new PointDTO(3, PieceType.KING.getStartColumnIndex()));
					applyMove(gameId, moveDTO);
				});
			}
		}
	}

	private void executeInSecondaryThread(Callback callback) {
		new Thread(() -> {
			try {
				callback.call();
			} catch (InterruptedException | GameNotFoundException | HistoryNotFoundException e) {
				e.printStackTrace();
			}
		}).start();
	}

	private interface Callback {
		void call() throws InterruptedException, GameNotFoundException, HistoryNotFoundException;
	}

}
