package com.example.chess.logic.objects.game;

import com.example.chess.enums.Side;

public interface IGame {

    Integer getPawnLongMoveColumnIndex(Side side);

    boolean isLongCastlingAvailable(Side side);

    boolean isShortCastlingAvailable(Side side);
}
