package com.example.chess.service.support;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import lombok.*;

@Getter
@Setter
public class MoveInfo {

    private CellDTO cellFrom;
    private PointDTO to;
    private PieceType attackedPiece;

    private int rating;

    public MoveInfo(CellDTO cellFrom, PointDTO to, PieceType attackedPiece) {
        this.cellFrom = cellFrom;
        this.to = to;
        this.attackedPiece = attackedPiece;
    }

    public PieceType getPieceFrom() {
        return cellFrom.getPieceType();
    }

    public PointDTO getFrom() {
        return cellFrom.getPoint();
    }
}
