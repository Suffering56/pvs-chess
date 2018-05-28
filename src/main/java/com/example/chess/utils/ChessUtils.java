package com.example.chess.utils;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.output.CellDTO;
import com.example.chess.entity.History;
import com.google.common.base.Preconditions;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

import static com.example.chess.service.GameService.BOARD_SIZE;

@Log4j2
public class ChessUtils {

	public static CellDTO getCell(List<List<CellDTO>> cellsMatrix, int rowIndex, int columnIndex) {
		checkPoint(rowIndex, columnIndex);
		return cellsMatrix.get(rowIndex).get(columnIndex);
	}

	public static CellDTO getCell(List<List<CellDTO>> cellsMatrix, PointDTO point) {
		checkPoint(point);
		return cellsMatrix.get(point.getRowIndex()).get(point.getColumnIndex());
	}

	public static void checkPoint(PointDTO point) {
		checkPoint(point.getRowIndex(), point.getColumnIndex());
	}

	public static void checkPoint(int rowIndex, int columnIndex) {
		String msg = "Out of board point";
		Preconditions.checkElementIndex(rowIndex, BOARD_SIZE, msg);
		Preconditions.checkElementIndex(columnIndex, BOARD_SIZE, msg);
	}

	public static void printCellsMatrix(List<List<CellDTO>> cellMatrix) {
		cellMatrix.forEach(row -> row.forEach(ChessUtils::printCell));
	}

	public static void printCell(CellDTO cell) {
		log.debug("cell[{},{}]: side = {}, piece = {}", cell.getRowIndex(), cell.getColumnIndex(),
				cell.getPiece().getSide(), cell.getPiece().getType());
	}

	public static void printHistory(History item) {
		log.debug("history[{},{}]: side = {}, piece = {}", item.getRowIndex(), item.getColumnIndex(),
				item.getPiece().getSide(), item.getPiece().getType());
	}

	public static List<List<CellDTO>> createEmptyCellsMatrix() {
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

	public static List<List<CellDTO>> createCellsMatrixByHistory(List<History> historyList) {
		List<List<CellDTO>> cellsMatrix = ChessUtils.createEmptyCellsMatrix();

		for (History item : historyList) {
			CellDTO cell = cellsMatrix.get(item.getRowIndex()).get(item.getColumnIndex());
			cell.setPiece(item.getPiece());
		}

		return cellsMatrix;
	}
}
