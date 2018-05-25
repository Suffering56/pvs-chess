package com.example.chess.service.impl;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.output.CellDTO;
import com.example.chess.enums.Side;
import com.example.chess.service.GameService;
import com.example.chess.service.MoveService;
import com.example.chess.utils.ChessUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service //prototype
public class MoveServiceImpl implements MoveService {

	private List<List<CellDTO>> cellsMatrix;
	private CellDTO selectedCell;
	private int selectedRow;
	private int selectedColumn;
	private Side alliedSide;
	private Side enemySide;

	@Override
	public List<PointDTO> getAvailableMoves(List<List<CellDTO>> cellsMatrix, PointDTO point) {
		this.cellsMatrix = cellsMatrix;
		this.selectedCell = ChessUtils.getCell(cellsMatrix, point);
		this.selectedRow = selectedCell.getRowIndex();
		this.selectedColumn = selectedCell.getColumnIndex();
		this.alliedSide = selectedCell.getSide();
		this.enemySide = getEnemySide(selectedCell);

		switch (selectedCell.getPiece()) {
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

		return new ArrayList<>();
	}

	private List<PointDTO> getMovesForPawn() {
		List<PointDTO> result = new ArrayList<>();

		int vector = 1;
		if (selectedCell.getSide() == Side.black) {
			vector = -1;
		}

		boolean isFirstMove = false;
		if (selectedRow == 1 || selectedRow == 6) {
			isFirstMove = true;
		}

		@SuppressWarnings("PointlessArithmeticExpression")
		CellDTO cell = getSelectedCell(selectedRow + 1 * vector, selectedColumn);
		boolean isAdded = addPawnMove(result, cell, false);

		if (isFirstMove && isAdded) {
			cell = getSelectedCell(selectedRow + 2 * vector, selectedColumn);
			addPawnMove(result, cell, false);
		}

		//attack
		cell = getSelectedCell(selectedRow + vector, selectedColumn + 1);
		addPawnMove(result, cell, true);

		cell = getSelectedCell(selectedRow + vector, selectedColumn - 1);
		addPawnMove(result, cell, true);

		//TODO: реализовать взятие на проходе

		return result;
	}

	private List<PointDTO> getMovesForKnight() {
		List<PointDTO> result = new ArrayList<>();

		checkAndAddKnightMove(result, 1, 2);
		checkAndAddKnightMove(result, 2, 1);
		checkAndAddKnightMove(result, 1, -2);
		checkAndAddKnightMove(result, 2, -1);
		checkAndAddKnightMove(result, -1, 2);
		checkAndAddKnightMove(result, -2, 1);
		checkAndAddKnightMove(result, -1, -2);
		checkAndAddKnightMove(result, -2, -1);

		return result;
	}

	private void checkAndAddKnightMove(List<PointDTO> resultMovesList, int rowOffset, int columnOffset) {
		CellDTO cell = getSelectedCell(selectedRow + rowOffset, selectedColumn + columnOffset);
		if (cell != null && cell.getSide() != alliedSide) {
			resultMovesList.add(cell);
		}
	}

	private List<PointDTO> getMovesForBishop() {
		List<PointDTO> result = new ArrayList<>();

		addAvailableMovesForRay(result, 1, 1);
		addAvailableMovesForRay(result, -1, 1);
		addAvailableMovesForRay(result, 1, -1);
		addAvailableMovesForRay(result, -1, -1);

		return result;
	}

	private List<PointDTO> getMovesForRook() {
		List<PointDTO> result = new ArrayList<>();

		addAvailableMovesForRay(result, 1, 0);
		addAvailableMovesForRay(result, -1, 0);
		addAvailableMovesForRay(result, 0, 1);
		addAvailableMovesForRay(result, 0, -1);

		return result;
	}

	private List<PointDTO> getMovesForQueen() {
		List<PointDTO> result = new ArrayList<>();

		result.addAll(getMovesForRook());
		result.addAll(getMovesForBishop());

		return result;
	}

	private List<PointDTO> getMovesForKing() {
		List<PointDTO> result = new ArrayList<>();

		addAvailableMovesForRay(result, 1, 0, 1);
		addAvailableMovesForRay(result, -1, 0, 1);
		addAvailableMovesForRay(result, 0, 1, 1);
		addAvailableMovesForRay(result, 0, -1, 1);
		addAvailableMovesForRay(result, 1, 1, 1);
		addAvailableMovesForRay(result, -1, 1, 1);
		addAvailableMovesForRay(result, 1, -1, 1);
		addAvailableMovesForRay(result, -1, -1, 1);

		//TODO: реализовать рокировку

		return result;
	}

	private void addAvailableMovesForRay(List<PointDTO> resultMovesList, int rowVector, int columnVector) {
		addAvailableMovesForRay(resultMovesList, rowVector, columnVector, 7);
	}

	private void addAvailableMovesForRay(List<PointDTO> resultMovesList, int rowVector, int columnVector, int rayLength) {
		for (int i = 1; i < rayLength + 1; i++) {
			CellDTO cell = getSelectedCell(selectedRow + rowVector * i, selectedColumn + columnVector * i);
			if (!addMove(resultMovesList, cell)) {
				break;
			}
		}
	}

	private boolean addMove(List<PointDTO> moves, CellDTO cell) {
		if (cell == null || cell.getSide() == alliedSide) {
			//IndexOutOfBounds
			return false;
		}

		moves.add(cell);
		return cell.getSide() != enemySide;
	}

	private boolean addPawnMove(List<PointDTO> moves, CellDTO cell, boolean isAttack) {
		if (cell == null || cell.getSide() == alliedSide) {
			//IndexOutOfBounds
			return false;
		}

		if (isAttack) {
			if (cell.getSide() == enemySide) {
				moves.add(cell);
			}
			return false;
		} else {
			if (cell.getSide() == enemySide) {
				return false;
			} else {
				moves.add(cell);
				return true;
			}
		}
	}


	private Side getEnemySide(CellDTO selectedCell) {
		Side enemySide = Side.black;
		if (selectedCell.getSide() == Side.black) {
			enemySide = Side.white;
		}
		return enemySide;
	}

	private CellDTO getSelectedCell(int rowIndex, int columnIndex) {
		if (rowIndex >= 0 && rowIndex < GameService.BOARD_SIZE && columnIndex >= 0 && columnIndex < GameService.BOARD_SIZE) {
			return cellsMatrix.get(rowIndex).get(columnIndex);
		} else {
			return null;
		}
	}

}
