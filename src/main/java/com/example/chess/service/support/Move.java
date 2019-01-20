package com.example.chess.service.support;

import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;

public interface Move {

    PointDTO getPointFrom();

    PointDTO getPointTo();

    boolean isCastling();

    boolean isLongCastling();

    boolean isShortCastling();

    boolean isLongPawnMove();

    boolean isPawnAttacks();

    int getRowIndexFrom();

    int getColumnIndexFrom();

    int getRowIndexTo();

    int getColumnIndexTo();

    PieceType getPieceFromPawn();
}
