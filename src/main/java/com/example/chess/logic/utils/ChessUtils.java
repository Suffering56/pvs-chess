package com.example.chess.logic.utils;

import com.example.chess.dto.CellDTO;
import com.example.chess.enums.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.Move;
import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("WeakerAccess")
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

    public static boolean isCastling(Move move, PieceType pieceType) {
        return pieceType == PieceType.KING && Math.abs(move.getColumnIndexFrom() - move.getColumnIndexTo()) == 2;
    }

    public static boolean isLongCastling(Move move, PieceType pieceType) {
        return pieceType == PieceType.KING && move.getColumnIndexFrom() - move.getColumnIndexTo() == -2;
    }

    public static boolean isShortCastling(Move move, PieceType pieceType) {
        return pieceType == PieceType.KING && move.getColumnIndexFrom() - move.getColumnIndexTo() == 2;
    }

    public static boolean isPawnAttacks(Move move, PieceType pieceType) {
        return pieceType == PieceType.PAWN && move.getColumnIndexFrom() != move.getColumnIndexTo();
    }

    public static boolean isLongPawnMove(Move move, PieceType pieceType) {
        return pieceType == PieceType.PAWN && Math.abs(move.getRowIndexFrom() - move.getRowIndexTo()) == 2;
    }

    public static boolean isPawnTransformation(Move move, PieceType pieceType) {
        return pieceType == PieceType.PAWN && (move.getRowIndexTo() == 0 || move.getRowIndexTo() == 7);
    }

    public static boolean isEnPassant(CellsMatrix matrix, Move move, PieceType pieceType) {
        if (isPawnAttacks(move, pieceType)) {
            CellDTO cellTo = matrix.getCell(move.getRowIndexTo(), move.getColumnIndexTo());
            if (cellTo == null) {
                return matrix.getCell(move.getRowIndexFrom(), move.getColumnIndexTo()) != null;
            }
        }
        return false;
    }


    public static boolean isCastling(Move move, Piece pieceFrom) {
        return isCastling(move, pieceFrom.getType());
    }

    public static boolean isLongCastling(Move move, Piece pieceFrom) {
        return isLongCastling(move, pieceFrom.getType());
    }

    public static boolean isShortCastling(Move move, Piece pieceFrom) {
        return isShortCastling(move, pieceFrom.getType());
    }

    public static boolean isPawnAttacks(Move move, Piece pieceFrom) {
        return isPawnAttacks(move, pieceFrom.getType());
    }

    public static boolean isLongPawnMove(Move move, Piece pieceFrom) {
        return isLongPawnMove(move, pieceFrom.getType());
    }

    public static boolean isPawnTransformation(Move move, Piece pieceFrom) {
        return isPawnTransformation(move, pieceFrom.getType());
    }

    public static boolean isEnPassant(CellsMatrix matrix, Move move, Piece pieceFrom) {
        return isEnPassant(matrix, move, pieceFrom.getType());
    }
}
