package com.example.chess.service.support;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class MoveRating {

    private CellDTO cellFrom;
    private PointDTO pointTo;
    private PieceType attackedPiece;

    private int rating;

    public MoveRating(CellDTO cellFrom, PointDTO pointTo, PieceType attackedPiece) {
        this.cellFrom = cellFrom;
        this.pointTo = pointTo;
        this.attackedPiece = attackedPiece;
    }

    public PieceType getPieceFrom() {
        return cellFrom.getPieceType();
    }
}
