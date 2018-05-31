package com.example.chess.utils;

import com.example.chess.dto.CellDTO;
import com.example.chess.entity.Piece;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MoveResult {

	private CellDTO cellFrom;
	private CellDTO cellTo;

	private Piece pieceFrom;
	private Piece pieceTo;

	public void rollbackMove() {
		cellFrom.setPiece(pieceFrom);
		cellTo.setPiece(pieceTo);
	}
}
