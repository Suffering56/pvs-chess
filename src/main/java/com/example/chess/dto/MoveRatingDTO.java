package com.example.chess.dto;

import com.example.chess.enums.PieceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MoveRatingDTO {

    private CellDTO cellFrom;
    private PointDTO pointTo;
    private PieceType attackedPiece;


    public PieceType getPieceFrom() {
        return cellFrom.getPieceType();
    }
}
