package com.example.chess.service.impl;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.output.CellDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.Side;
import com.example.chess.service.MoveService;
import com.example.chess.utils.ChessUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.chess.ChessConstants.BOARD_SIZE;

@Service //prototype
public class MoveServiceImpl implements MoveService {

	private Game game;
	private List<List<CellDTO>> cellsMatrix;
	private CellDTO selectedCell;

	private int selectedRow;
	private int selectedColumn;
	private Side alliedSide;
	private Side enemySide;

	boolean isExternalCall = true;

	@Override
	public Set<PointDTO> getAvailableMoves(Game game, List<List<CellDTO>> cellsMatrix, PointDTO point) {
		this.game = game;
		this.cellsMatrix = cellsMatrix;
		this.selectedCell = ChessUtils.getCell(cellsMatrix, point);

		this.selectedRow = selectedCell.getRowIndex();
		this.selectedColumn = selectedCell.getColumnIndex();
		this.alliedSide = selectedCell.getPieceSide();
		this.enemySide = getEnemySide(selectedCell);

		switch (selectedCell.getPieceType()) {
			case pawn: {
				return getMovesForPawn();
			}
			case knight: {
				return getMovesForKnight();
			}
			case bishop: {
				return getMovesForBishop();
			}
			case rook: {
				return getMovesForRook();
			}
			case queen: {
				return getMovesForQueen();
			}
			case king: {
				return getMovesForKing();
			}
		}

		return new HashSet<>();
	}

	private Set<PointDTO> getMovesForPawn() {
		Set<PointDTO> moves = new HashSet<>();

		int vector = 1;
		if (selectedCell.getPieceSide() == Side.black) {
			vector = -1;
		}

		boolean isFirstMove = false;
		if (selectedRow == 1 || selectedRow == 6) {
			isFirstMove = true;
		}

		@SuppressWarnings("PointlessArithmeticExpression")
		CellDTO cell = getCell(selectedRow + 1 * vector, selectedColumn);
		boolean isAdded = addPawnMove(moves, cell, false);

		if (isFirstMove && isAdded) {
			cell = getCell(selectedRow + 2 * vector, selectedColumn);
			addPawnMove(moves, cell, false);
		}

		//attack
		cell = getCell(selectedRow + vector, selectedColumn + 1);
		addPawnMove(moves, cell, true);

		cell = getCell(selectedRow + vector, selectedColumn - 1);
		addPawnMove(moves, cell, true);

		//TODO: реализовать взятие на проходе

		return moves;
	}

	private Set<PointDTO> getMovesForKnight() {
		Set<PointDTO> moves = new HashSet<>();

		checkAndAddKnightMove(moves, 1, 2);
		checkAndAddKnightMove(moves, 2, 1);
		checkAndAddKnightMove(moves, 1, -2);
		checkAndAddKnightMove(moves, 2, -1);
		checkAndAddKnightMove(moves, -1, 2);
		checkAndAddKnightMove(moves, -2, 1);
		checkAndAddKnightMove(moves, -1, -2);
		checkAndAddKnightMove(moves, -2, -1);

		return moves;
	}

	private void checkAndAddKnightMove(Set<PointDTO> moves, int rowOffset, int columnOffset) {
		CellDTO cell = getCell(selectedRow + rowOffset, selectedColumn + columnOffset);
		if (cell != null && cell.getPieceSide() != alliedSide) {
			moves.add(cell.generatePoint());
		}
	}

	private Set<PointDTO> getMovesForBishop() {
		Set<PointDTO> moves = new HashSet<>();

		addAvailableMovesForRay(moves, 1, 1);
		addAvailableMovesForRay(moves, -1, 1);
		addAvailableMovesForRay(moves, 1, -1);
		addAvailableMovesForRay(moves, -1, -1);

		return moves;
	}

	private Set<PointDTO> getMovesForRook() {
		Set<PointDTO> moves = new HashSet<>();

		addAvailableMovesForRay(moves, 1, 0);
		addAvailableMovesForRay(moves, -1, 0);
		addAvailableMovesForRay(moves, 0, 1);
		addAvailableMovesForRay(moves, 0, -1);

		return moves;
	}

	private Set<PointDTO> getMovesForQueen() {
		Set<PointDTO> moves = new HashSet<>();

		moves.addAll(getMovesForRook());
		moves.addAll(getMovesForBishop());

		return moves;
	}

	private Set<PointDTO> getMovesForKing() {
		Set<PointDTO> moves = new HashSet<>();



		addAvailableMovesForRay(moves, 1, 0, 1);
		addAvailableMovesForRay(moves, -1, 0, 1);
		addAvailableMovesForRay(moves, 0, 1, 1);
		addAvailableMovesForRay(moves, 0, -1, 1);
		addAvailableMovesForRay(moves, 1, 1, 1);
		addAvailableMovesForRay(moves, -1, 1, 1);
		addAvailableMovesForRay(moves, 1, -1, 1);
		addAvailableMovesForRay(moves, -1, -1, 1);

		//TODO: реализовать рокировку
		if (game.isShortCastlingAvailableForSide(alliedSide)) {
			if (isEmptyCellsBySelectedRow(1, 2)) {
				addMove(moves, getCell(selectedRow, selectedColumn - 2));
			}
		}
		if (game.isLongCastlingAvailableForSide(alliedSide)) {
			if (isEmptyCellsBySelectedRow(4, 5, 6)) {
				addMove(moves, getCell(selectedRow, selectedColumn + 2));
			}
		}

		Set<PointDTO> allEnemyMoves = getAllAvailableMovesForSide(enemySide);
		return moves.stream()
				.filter(point -> !allEnemyMoves.contains(point))
				.collect(Collectors.toSet());
	}

	private boolean isEmptyCellsBySelectedRow(int... columnIndexes) {
		for (int columnIndex : columnIndexes) {
			if (!isEmptyCell(selectedRow, columnIndex)) {
				return false;
			}
		}

		return true;
	}

	private boolean isEmptyCell(int rowIndex, int columnIndex) {
		CellDTO cell = ChessUtils.getCell(cellsMatrix, rowIndex, columnIndex);
		return cell.getPiece() == null;
	}

	private void addAvailableMovesForRay(Set<PointDTO> moves, int rowVector, int columnVector) {
		addAvailableMovesForRay(moves, rowVector, columnVector, 7);
	}

	private void addAvailableMovesForRay(Set<PointDTO> moves, int rowVector, int columnVector, int rayLength) {
		for (int i = 1; i < rayLength + 1; i++) {
			CellDTO cell = getCell(selectedRow + rowVector * i, selectedColumn + columnVector * i);
			if (!addMove(moves, cell)) {
				break;
			}
		}
	}

	private boolean addMove(Set<PointDTO> moves, CellDTO cell) {
		if (cell == null || cell.getPieceSide() == alliedSide) {
			//IndexOutOfBounds
			return false;
		}

		moves.add(cell.generatePoint());
		return cell.getPieceSide() != enemySide;
	}

	private boolean addPawnMove(Set<PointDTO> moves, CellDTO cell, boolean isAttack) {
		if (cell == null || cell.getPieceSide() == alliedSide) {
			//IndexOutOfBounds
			return false;
		}

		if (isAttack) {
			if (cell.getPieceSide() == enemySide) {
				moves.add(cell.generatePoint());
			}
			return false;
		} else {
			if (cell.getPieceSide() == enemySide) {
				return false;
			} else {
				moves.add(cell.generatePoint());
				return true;
			}
		}
	}

	private Side getEnemySide(CellDTO selectedCell) {
		Side enemySide = Side.black;
		if (selectedCell.getPieceSide() == Side.black) {
			enemySide = Side.white;
		}
		return enemySide;
	}

	private CellDTO getCell(int rowIndex, int columnIndex) {
		if (rowIndex >= 0 && rowIndex < BOARD_SIZE && columnIndex >= 0 && columnIndex < BOARD_SIZE) {
			return cellsMatrix.get(rowIndex).get(columnIndex);
		} else {
			return null;
		}
	}

	private Set<PointDTO> getAllAvailableMovesForSide(Side expectedSide) {
		if (isExternalCall) {
			isExternalCall = false;
			return cellsMatrix.stream()
					.flatMap(List::stream)
					.filter(cell -> cell.getPieceSide() == expectedSide)
					.map(cell -> getAvailableMoves(game, cellsMatrix, cell.generatePoint()))
					.flatMap(Set::stream)
					.collect(Collectors.toSet());

		}
		return new HashSet<>();
	}

}
