package com.example.chess.logic.utils;

import com.example.chess.enums.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ChessUtils {

    public static final BiIntFunction<Piece> START_ARRANGEMENT_GENERATOR = (rowIndex, columnIndex) -> {
        Side side = null;
        if (rowIndex == 0 || rowIndex == 1) {
            side = Side.WHITE;
        } else if (rowIndex == 7 || rowIndex == 6) {
            side = Side.BLACK;
        }

        PieceType pieceType = null;
        if (rowIndex == 1 || rowIndex == 6) {
            pieceType = PieceType.PAWN;
        } else if (rowIndex == 0 || rowIndex == 7) {
            if (columnIndex == 0 || columnIndex == 7) {
                pieceType = PieceType.ROOK;
            } else if (columnIndex == 1 || columnIndex == 6) {
                pieceType = PieceType.KNIGHT;
            } else if (columnIndex == 2 || columnIndex == 5) {
                pieceType = PieceType.BISHOP;
            } else if (columnIndex == 3) {
                pieceType = PieceType.KING;
            } else {  //columnIndex == 4
                pieceType = PieceType.QUEEN;
            }
        }

        if (side != null) {
            return Piece.of(side, pieceType);
        }
        return null;
    };
}
