package com.example.chess.dto;

import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Immutable
 */
@Getter
@ToString
@SuppressWarnings("WeakerAccess")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MoveDTO {

    private final PointDTO from;
    private final PointDTO to;
    private final PieceType promotionPieceType;

    public static MoveDTO valueOf(PointDTO from, PointDTO to, PieceType promotionPieceType) {
        return new MoveDTO(from, to, promotionPieceType);
    }

    public boolean isCastling() {
        int diff = from.getColumnIndex() - to.getColumnIndex();
        return Math.abs(diff) == 2;
    }

    public boolean isLongCastling() {
        int diff = from.getColumnIndex() - to.getColumnIndex();
        if (diff != 2) {

        }
        return diff < 0;
    }

    public boolean isShortCastling() {
        return !isLongCastling();
    }

    public boolean isLongPawnMove() {
        int diff = from.getRowIndex() - to.getRowIndex();
        return Math.abs(diff) == 2;
    }

    public boolean isPawnAttacks() {
        return !Objects.equals(from.getColumnIndex(), to.getColumnIndex());
    }

    public boolean isEnPassant(Piece attackedPiece) {
        return attackedPiece == null && isPawnAttacks();
    }
}
