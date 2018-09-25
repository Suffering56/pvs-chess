package com.example.chess.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public enum PieceType {
    PAWN(1), KNIGHT(3), BISHOP(3), ROOK(5), QUEEN(9, 4), KING(9999, 3);

    private int value;
    private int startColumnIndex;

    PieceType(int value, int startColumnIndex) {
        this.startColumnIndex = startColumnIndex;
        this.value = value;
    }

    PieceType(int value) {
        this.value = value;
    }
}
