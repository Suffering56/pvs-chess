package com.example.chess.service.support;

import com.example.chess.enums.Side;

public interface Gameplay {

    Integer getPawnLongMoveColumnIndex(Side side);

    boolean isLongCastlingAvailable(Side side);

    boolean isShortCastlingAvailable(Side side);
}
