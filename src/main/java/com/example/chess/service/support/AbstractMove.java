package com.example.chess.service.support;

import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.beans.Transient;

public abstract class AbstractMove implements Move {

    @Override
    public abstract PointDTO getPointFrom();

    @Override
    public abstract PointDTO getPointTo();

    @Override
    public abstract PieceType getPieceFromPawn();

    @Override
    public abstract int getRowIndexFrom();

    @Override
    public abstract int getColumnIndexFrom();

    @Override
    public abstract int getRowIndexTo();

    @Override
    public abstract int getColumnIndexTo();

    @Override
    @Transient
    @JsonIgnore
    public boolean isCastling() {
        int diff = getPointFrom().getColumnIndex() - getPointTo().getColumnIndex();
        return Math.abs(diff) == 2;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isLongCastling() {
        int diff = getPointFrom().getColumnIndex() - getPointTo().getColumnIndex();
        return diff < 0;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isShortCastling() {
        return !isLongCastling();
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isLongPawnMove() {
        int diff = getPointFrom().getRowIndex() - getPointTo().getRowIndex();
        return Math.abs(diff) == 2;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isPawnAttacks() {
        return getPointFrom().getColumnIndex() != getPointTo().getColumnIndex();
    }
}
