package com.example.chess.utils;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.output.CellDTO;
import com.example.chess.entity.History;
import com.example.chess.service.GameService;
import com.google.common.base.Preconditions;
import lombok.extern.log4j.Log4j2;

import java.util.List;

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
		Preconditions.checkElementIndex(rowIndex, GameService.BOARD_SIZE, msg);
		Preconditions.checkElementIndex(columnIndex, GameService.BOARD_SIZE, msg);
	}

	public static void printCellsMatrix(List<List<CellDTO>> cellMatrix) {
		cellMatrix.forEach(row -> row.forEach(ChessUtils::printCell));
	}

	public static void printCell(CellDTO cell) {
		log.debug("cell[{},{}]: side = {}, piece = {}", cell.getRowIndex(), cell.getColumnIndex(), cell.getSide(), cell.getPiece());
	}

	public static void printHistory(History item) {
		log.debug("history[{},{}]: side = {}, piece = {}", item.getRowIndex(), item.getColumnIndex(),
				item.getPiece().getSide(), item.getPiece().getType());
	}
}
