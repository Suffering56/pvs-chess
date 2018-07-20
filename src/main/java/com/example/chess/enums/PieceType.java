package com.example.chess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum PieceType {
	pawn, knight, bishop, rook, queen(4), king(3);

	private int startColumnIndex;
}
