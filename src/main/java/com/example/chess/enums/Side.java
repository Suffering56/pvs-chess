package com.example.chess.enums;

public enum Side {
	WHITE, BLACK;

	public Side reverse() {
		if (this == WHITE) {
			return BLACK;
		} else {
			return WHITE;
		}
	}
}
