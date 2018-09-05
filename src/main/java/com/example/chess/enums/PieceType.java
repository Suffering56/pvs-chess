package com.example.chess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum PieceType {
	PAWN, KNIGHT, BISHOP, ROOK, QUEEN(4), KING(3);

	private int startColumnIndex;
}
