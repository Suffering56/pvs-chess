package com.example.chess.enums;

public enum Side {
	white, black;

	public Side reverse() {
		if (this == white) {
			return black;
		} else {
			return white;
		}
	}
}
