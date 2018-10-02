package com.example.chess.service.support;

import lombok.Getter;

@Getter
public enum RatingParam {
    EXCHANGE_DIFF, ATTACK_DEFENSELESS_PIECE, PIECE_RESCUE;

    private final int factor;

    RatingParam() {
        this(1);
    }

    RatingParam(int factor) {
        this.factor = factor;
    }
}
