package com.example.chess.dto;

import com.example.chess.enums.PieceType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class MoveRatingDTO {

    private CellDTO cellFrom;
    private PointDTO pointTo;
    private PieceType attackedPiece;

    private int rating;

    public MoveRatingDTO(CellDTO cellFrom, PointDTO pointTo, PieceType attackedPiece) {
        this.cellFrom = cellFrom;
        this.pointTo = pointTo;
        this.attackedPiece = attackedPiece;
    }

    public PieceType getPieceFrom() {
        return cellFrom.getPieceType();
    }
}
