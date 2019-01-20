package com.example.chess.dto;

import com.example.chess.entity.History;
import com.example.chess.enums.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.logic.objects.move.AbstractMove;
import com.example.chess.logic.utils.Immutable;
import com.example.chess.logic.utils.CommonUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class MoveDTO extends AbstractMove implements Immutable {

    PointDTO from;
    PointDTO to;
    PieceType pieceFromPawn;

    @JsonCreator
    public static MoveDTO valueOf(@JsonProperty PointDTO from, @JsonProperty PointDTO to, @JsonProperty PieceType pieceFromPawn) {
        return new MoveDTO(from, to, pieceFromPawn);
    }

    @Override
    public PointDTO getPointFrom() {
        return from;
    }

    @Override
    public PointDTO getPointTo() {
        return to;
    }

    @Override
    @JsonIgnore
    public int getRowIndexFrom() {
        return from.getRowIndex();
    }

    @Override
    @JsonIgnore
    public int getColumnIndexFrom() {
        return from.getColumnIndex();
    }

    @Override
    @JsonIgnore
    public int getRowIndexTo() {
        return to.getRowIndex();
    }

    @Override
    @JsonIgnore
    public int getColumnIndexTo() {
        return to.getColumnIndex();
    }

    @JsonIgnore
    public History toHistory(Long gameId, int position, Piece pieceFrom) {
        return History.builder()
                .gameId(gameId)
                .position(position)
                .rowIndexFrom(getRowIndexFrom())
                .columnIndexFrom(getColumnIndexFrom())
                .rowIndexTo(getRowIndexTo())
                .columnIndexTo(getColumnIndexTo())
                .pieceFromPawn(getPieceFromPawn())
                .description(createDescription(pieceFrom))
                .build();
    }

    @JsonIgnore
    private String createDescription(Piece pieceFrom) {
        String description = CommonUtils.getPieceName(pieceFrom.getType(), true);
        description += String.format(": %s -> %s (%s)", from, to, pieceFrom.getSide());
        return description;
    }
}
