package com.example.chess.enums;

import lombok.Getter;

@Getter
public enum Side {
    WHITE(1), BLACK(-1);

    private int pawnMoveVector;

    Side(int pawnMoveVector) {
        this.pawnMoveVector = pawnMoveVector;
    }

    public Side reverse() {
        if (this == WHITE) {
            return BLACK;
        }
        return WHITE;
    }
}
