package com.example.chess.dto;

import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Immutable
 */
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CellDTO {

    private final Integer rowIndex;
    private final Integer columnIndex;
    private final Piece piece;

    public static CellDTO valueOf(Integer rowIndex, Integer columnIndex, Piece piece) {
        return new CellDTO(rowIndex, columnIndex, piece);
    }

    public CellDTO switchPiece(Piece piece) {
        return new CellDTO(rowIndex, columnIndex, piece);
    }

    public Side getPieceSide() {
        if (getPiece() == null) {
            return null;
        }
        return getPiece().getSide();
    }

    public PieceType getPieceType() {
        if (getPiece() == null) {
            return null;
        }
        return getPiece().getType();
    }

    public PointDTO getPoint() {
        return PointDTO.valueOf(rowIndex, columnIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CellDTO cellDTO = (CellDTO) o;

        return new EqualsBuilder()
                .append(piece, cellDTO.piece)
                .append(rowIndex, cellDTO.rowIndex)
                .append(columnIndex, cellDTO.columnIndex)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(piece)
                .append(rowIndex)
                .append(columnIndex)
                .toHashCode();
    }
}
