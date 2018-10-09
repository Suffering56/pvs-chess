package com.example.chess.dto;

import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.service.support.Immutable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MoveDTO implements Immutable {

    private final PointDTO from;
    private final PointDTO to;
    private final PieceType promotionPieceType;

    @JsonCreator
    public static MoveDTO valueOf(@JsonProperty PointDTO from, @JsonProperty PointDTO to, @JsonProperty PieceType promotionPieceType) {
        return new MoveDTO(from, to, promotionPieceType);
    }

    @JsonIgnore
    public boolean isCastling() {
        int diff = from.getColumnIndex() - to.getColumnIndex();
        return Math.abs(diff) == 2;
    }

    @JsonIgnore
    public boolean isLongCastling() {
        int diff = from.getColumnIndex() - to.getColumnIndex();
        return diff < 0;
    }

    @JsonIgnore
    public boolean isShortCastling() {
        return !isLongCastling();
    }

    @JsonIgnore
    public boolean isLongPawnMove() {
        int diff = from.getRowIndex() - to.getRowIndex();
        return Math.abs(diff) == 2;
    }

    @JsonIgnore
    public boolean isPawnAttacks() {
        return !Objects.equals(from.getColumnIndex(), to.getColumnIndex());
    }

    @JsonIgnore
    public boolean isEnPassant(Piece attackedPiece) {
        return attackedPiece == null && isPawnAttacks();
    }
}
