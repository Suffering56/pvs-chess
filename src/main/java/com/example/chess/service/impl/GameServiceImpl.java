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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.function.Function.identity;

@Log4j2
@Service
public class GameServiceImpl implements GameService {

	private final Supplier<MoveService> moveService;
	private final GameRepository gameRepository;
	private final HistoryRepository historyRepository;
	private final PieceRepository pieceRepository;

	private Map<Long, Piece> piecesByIdMap;
	private Map<Side, Map<PieceType, Piece>>  piecesBySideAndTypeMap;

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

		return moveService.get().getAvailableMoves(cellsMatrix, point);
	}

	@Override
	@Transactional
	@Profile
	public ArrangementDTO applyMove(long gameId, MoveDTO dto) throws HistoryNotFoundException, GameNotFoundException {
		Game game = findAndCheckGame(gameId);

		List<History> currentHistory;
		if (game.getPosition() == 0) {
			currentHistory = createStartHistory(gameId);
		} else {
			currentHistory = findHistoryByGameIdAndPosition(gameId, game.getPosition());
		}

		List<List<CellDTO>> cellMatrix = createCellsMatrixByHistory(currentHistory);

		List<History> nextHistory = new ArrayList<>();

		for (History current : currentHistory) {
			boolean isSelected = current.getRowIndex().equals(dto.getFrom().getRowIndex()) && current.getColumnIndex().equals(dto.getFrom().getColumnIndex());
			History item = null;

			if (isSelected) {
				item = History.createForNextPosition(current);
				item.setRowIndex(dto.getTo().getRowIndex());
				item.setColumnIndex(dto.getTo().getColumnIndex());
			} else {
				boolean isTo = current.getRowIndex().equals(dto.getTo().getRowIndex()) && current.getColumnIndex().equals(dto.getTo().getColumnIndex());
				if (!isTo) {
					item = History.createForNextPosition(current);
				}
			}

			if (item != null) {
				nextHistory.add(item);
			}
		}

		int newPosition = game.getPosition() + 1;
		doMoveInMatrix(cellMatrix, dto);
		game.setPosition(newPosition);

		historyRepository.save(nextHistory);
		gameRepository.save(game);

		return new ArrangementDTO(newPosition, cellMatrix);
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

	private void doMoveInMatrix(List<List<CellDTO>> cellMatrix, MoveDTO move) {
		CellDTO cellFrom = ChessUtils.getCell(cellMatrix, move.getFrom());
		CellDTO cellTo = ChessUtils.getCell(cellMatrix, move.getTo());

		cellTo.setSide(cellFrom.getSide());
		cellTo.setPiece(cellFrom.getPiece());

		cellFrom.setSide(null);
		cellFrom.setPiece(null);
	}

	private List<List<CellDTO>> createCellsMatrixByGame(Game game) throws HistoryNotFoundException {
		return createCellsMatrixByGameIdAndPosition(game.getId(), game.getPosition());
	}

	private List<List<CellDTO>> createCellsMatrixByGameIdAndPosition(long gameId, int position) throws HistoryNotFoundException {
		if (position == 0) {
			return createStartCellsMatrix(gameId);
		}

		List<History> historyList = findHistoryByGameIdAndPosition(gameId, position);
		return createCellsMatrixByHistory(historyList);
	}

	private List<History> findHistoryByGameIdAndPosition(long gameId, int position) throws HistoryNotFoundException {
		List<History> historyList = historyRepository.findByGameIdAndPositionOrderByRowIndexAscColumnIndexAsc(gameId, position);
		if (historyList.isEmpty()) {
			throw new HistoryNotFoundException();
		}
		return historyList;
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
		return createCellsMatrixByHistory(createStartHistory(gameId));
	}

	private List<List<CellDTO>> createCellsMatrixByHistory(List<History> historyList) {
		List<List<CellDTO>> cellsMatrix = createEmptyCellsMatrix();

		for (History item : historyList) {
			CellDTO cell = cellsMatrix.get(item.getRowIndex()).get(item.getColumnIndex());
			cell.setSide(item.getPiece().getSide());
			cell.setPiece(item.getPiece().getType());
		}

		return cellsMatrix;
	}

	private List<List<CellDTO>> createEmptyCellsMatrix() {
		List<List<CellDTO>> cellsMatrix = new ArrayList<>(BOARD_SIZE);

		//1-8
		for (int rowIndex = 0; rowIndex < BOARD_SIZE; rowIndex++) {
			List<CellDTO> rowCells = new ArrayList<>(BOARD_SIZE);

			//A-H
			for (int columnIndex = 0; columnIndex < BOARD_SIZE; columnIndex++) {
				rowCells.add(new CellDTO(rowIndex, columnIndex));
			}
			cellsMatrix.add(rowCells);
		}

		return cellsMatrix;
	}


	private Piece findPieceBySideAndType(Side side, PieceType type) {
		return piecesBySideAndTypeMap.get(side).get(type);
	}
}
