package com.example.chess.service.impl;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.PointDTO;
import com.example.chess.dto.input.MoveDTO;
import com.example.chess.dto.output.CellDTO;
import com.example.chess.dto.output.ArrangementDTO;
import com.example.chess.entity.Game;
import com.example.chess.entity.History;
import com.example.chess.entity.Piece;
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
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
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

	private Map<Integer, Piece> piecesByIdMap;
	private Map<Side, Map<PieceType, Piece>> piecesBySideAndTypeMap;

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

		piecesByIdMap = StreamSupport.stream(pieces.spliterator(), false)
				.collect(Collectors.toMap(Piece::getId, identity()));

		piecesBySideAndTypeMap = StreamSupport.stream(pieces.spliterator(), false)
				.collect(Collectors.groupingBy(Piece::getSide,
						Collectors.toMap(Piece::getType, identity())));
	}

	@Override
	public Game findAndCheckGame(long gameId) throws GameNotFoundException {
		Game game = gameRepository.findOne(gameId);
		if (game == null) {
			throw new GameNotFoundException();
		}
		return game;
	}

	@Override
	public List<PointDTO> getAvailableMoves(long gameId, PointDTO point) throws GameNotFoundException, HistoryNotFoundException {
		Game game = findAndCheckGame(gameId);
		List<List<CellDTO>> cellsMatrix = createCellsMatrixByGame(game);

		return moveService.get().getAvailableMoves(game, cellsMatrix, point);
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
		Piece moveablePiece = executeMove(cellsMatrix, move);
		Side moveableSide = moveablePiece.getSide();

		if (moveablePiece.getType() == PieceType.king) {
			//do castling (only the rook moves)
			checkAndExecuteCastling(cellsMatrix, move);

			game.disableShortCasting(moveableSide);
			game.disableLongCasting(moveableSide);

		} else if (moveablePiece.getType() == PieceType.rook) {

			if (game.isShortCastlingAvailableForSide(moveableSide) && move.getFrom().getColumnIndex() == ROOK_SHORT_COLUMN_INDEX) {
				game.disableShortCasting(moveableSide);

			} else if (game.isLongCastlingAvailableForSide(moveableSide) && move.getFrom().getColumnIndex() == ROOK_LONG_COLUMN_INDEX) {
				game.disableLongCasting(moveableSide);
			}
		}

		List<History> afterMoveHistory = createHistoryByCellsMatrix(cellsMatrix, gameId, newPosition);
		game.setPosition(newPosition);

		historyRepository.save(afterMoveHistory);
		gameRepository.save(game);

		return new ArrangementDTO(newPosition, cellsMatrix);
	}

	private void checkAndExecuteCastling(List<List<CellDTO>> cellsMatrix, MoveDTO move) {
		int diff = move.getFrom().getColumnIndex() - move.getTo().getColumnIndex();

		if (Math.abs(diff) == 2) {	//is castling
			Integer kingFromColumnIndex = move.getFrom().getColumnIndex();

			//short
			PointDTO rookFrom = new PointDTO(move.getFrom().getRowIndex(), ROOK_SHORT_COLUMN_INDEX);
			PointDTO rookTo = new PointDTO(move.getFrom().getRowIndex(), kingFromColumnIndex - 1);

			//long
			if (diff < 0) {
				rookFrom.setColumnIndex(ROOK_LONG_COLUMN_INDEX);
				rookTo.setColumnIndex(kingFromColumnIndex + 1);
			}

			//move rook
			executeMove(cellsMatrix, new MoveDTO(rookFrom, rookTo));
		}
	}

	private Piece executeMove(List<List<CellDTO>> cellsMatrix, MoveDTO move) {
		CellDTO cellFrom = ChessUtils.getCell(cellsMatrix, move.getFrom());
		CellDTO cellTo = ChessUtils.getCell(cellsMatrix, move.getTo());

		Piece piece = cellFrom.getPiece();

		cellTo.setPiece(cellFrom.getPiece());
		cellFrom.setPiece(null);

		return piece;
	}

	@Override
	public ArrangementDTO getArrangementByPosition(long gameId, int position) throws HistoryNotFoundException {
		ArrangementDTO result = new ArrangementDTO();

		if (position > 0) {
			result.setCellsMatrix(createCellsMatrixByGameIdAndPosition(gameId, position));
		} else {
			result.setCellsMatrix(createStartCellsMatrix(gameId));
		}

		result.setPosition(position);

		return result;
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
					side = Side.white;
				} else if (rowIndex == 7 || rowIndex == 6) {
					side = Side.black;
				}

				PieceType pieceType = null;
				if (rowIndex == 1 || rowIndex == 6) {
					pieceType = PieceType.pawn;
				} else if (rowIndex == 0 || rowIndex == 7) {
					if (columnIndex == 0 || columnIndex == 7) {
						pieceType = PieceType.rook;
					} else if (columnIndex == 1 || columnIndex == 6) {
						pieceType = PieceType.knight;
					} else if (columnIndex == 2 || columnIndex == 5) {
						pieceType = PieceType.bishop;
					} else if (columnIndex == 3) {
						pieceType = PieceType.king;
					} else {  //columnIndex == 4
						pieceType = PieceType.queen;
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
}
