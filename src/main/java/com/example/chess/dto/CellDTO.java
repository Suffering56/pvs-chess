package com.example.chess.dto;

import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.support.Immutable;
import com.example.chess.utils.CommonUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CellDTO implements Immutable {

    private final Integer rowIndex;
    private final Integer columnIndex;
    private final Piece piece;

    @JsonCreator
    public static CellDTO valueOf(@JsonProperty Integer rowIndex, @JsonProperty Integer columnIndex, @JsonProperty Piece piece) {
        return new CellDTO(rowIndex, columnIndex, piece);
    }

    public CellDTO switchPiece(Piece piece) {
        return new CellDTO(rowIndex, columnIndex, piece);
    }

    @JsonIgnore
    public PointDTO getPoint() {
        return PointDTO.valueOf(rowIndex, columnIndex);
    }

    @JsonIgnore
    public Side getSide() {
        if (piece == null) {
            return null;
        }
        return piece.getSide();
    }

    @JsonIgnore
    public PieceType getPieceType() {
        if (piece == null) {
            return null;
        }
        return piece.getType();
    }

    @JsonIgnore
    public Side getEnemySide() {
        return piece.getSide().reverse();
    }

    @JsonIgnore
    public int getValue() {
        return piece.getType().getValue();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return piece == null;
    }

    public void requireNotEmpty() throws NullPointerException {
        Objects.requireNonNull(piece, "Cell is empty");
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

    @Override
    public String toString() {
        return getPoint() + ": " + CommonUtils.getPieceName(getPieceType(), true);
    }
}
