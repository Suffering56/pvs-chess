package com.example.chess.service.support;

import com.example.chess.Debug;
import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.History;
import com.example.chess.enums.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.KingNotFoundException;
import com.example.chess.utils.BiIntFunction;
import com.example.chess.utils.ChessUtils;
import com.example.chess.utils.CommonUtils;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.chess.ChessConstants.*;

public final class CellsMatrix implements Immutable {

    @Getter
    private final int position;
    private final List<List<CellDTO>> cellsMatrix;
    private Map<Side, PointDTO> kingPoints = new EnumMap<>(Side.class);

    private CellsMatrix(int position, BiIntFunction<Piece> pieceGenerator) {
        this.position = position;
        this.cellsMatrix = new ArrayList<>(BOARD_SIZE);

        //1-8
        for (int rowIndex = 0; rowIndex < BOARD_SIZE; rowIndex++) {
            List<CellDTO> rowCells = new ArrayList<>(BOARD_SIZE);

            //A-H
            for (int columnIndex = 0; columnIndex < BOARD_SIZE; columnIndex++) {

                Piece piece = pieceGenerator.apply(rowIndex, columnIndex);
                rowCells.add(CellDTO.valueOf(rowIndex, columnIndex, piece));

                if (piece != null && piece.getType() == PieceType.KING) {
                    kingPoints.put(piece.getSide(), PointDTO.valueOf(rowIndex, columnIndex));
                }
            }
            cellsMatrix.add(rowCells);
        }
    }

    public CellDTO getCell(int rowIndex, int columnIndex) {
        checkPoint(rowIndex, columnIndex);
        return cellsMatrix.get(rowIndex).get(columnIndex);
    }

    public CellDTO getCell(PointDTO point) {
        return getCell(point.getRowIndex(), point.getColumnIndex());
    }

    private void checkPoint(int rowIndex, int columnIndex) {
        Preconditions.checkElementIndex(rowIndex, BOARD_SIZE, "Out of board point");
        Preconditions.checkElementIndex(columnIndex, BOARD_SIZE, "Out of board point");
    }

    public MoveResult executeMove(Move move) {
        Debug.movesExecuted.incrementAndGet();

        CellsMatrix newMatrix = builder(position + 1, this)
                .executeMove(move)
                .build();

        return MoveResult.valueOf(this, newMatrix);
    }

    public ArrangementDTO generateArrangement(Side underCheckSide) {
        return new ArrangementDTO(position, cellsMatrix, underCheckSide);
    }

    public static CellsMatrix ofMatrix(int newPosition, CellsMatrix prevMatrix) {
        return builder(newPosition, prevMatrix).build();
    }

    private static Builder builder(int position, CellsMatrix prevMatrix) {
        return new CellsMatrix(position, (rowIndex, columnIndex) -> {
            CellDTO cell = prevMatrix.getCell(rowIndex, columnIndex);
            return cell.getPiece();
        }).new Builder();
    }

    public static CellsMatrix ofHistory(int newPosition, List<History> historyList) {
        return builder(newPosition, historyList).build();
    }

    private static Builder builder(int position, List<History> historyList) {
        Builder builder = new CellsMatrix(position, ChessUtils.START_ARRANGEMENT_GENERATOR).new Builder();
        historyList.forEach(builder::executeMove);
        return builder;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public class Builder {

        private Builder executeMove(Move move) {
            Piece pieceFrom = getCell(move.getPointFrom()).getPiece();

            if (isCastling(move, pieceFrom)) {
                return executeCastling(move, pieceFrom);
            } else if (isEnPassant(move, pieceFrom)) {
                return executeEnPassant(move, pieceFrom);
            } else {
                if (isPawnTransformation(move, pieceFrom)) {
                    Preconditions.checkNotNull(move.getPieceFromPawn(), "Piece from pawn can't be null");
                    pieceFrom = Piece.of(pieceFrom.getSide(), move.getPieceFromPawn());
                }
                return executeSimpleMove(move, pieceFrom);
            }
        }

        /**
         * Обычный ход.
         * <p>
         * description:
         * - Перемещаем фигуру из положения #from(X.Y) в положение #to(X.Y).
         * - Убираем фигуру из #from.
         * - В #to ставим ту фигуру, которой ходим (кроме случаев превращения пешки)
         * <p>
         * Условие: это не рокировка и не взятие на проходе.
         *
         * @return MoveResult (contains new matrix with updated state)
         */
        private Builder executeSimpleMove(Move move, Piece pieceFrom) {
            return cutPiece(move.getRowIndexFrom(), move.getColumnIndexFrom())              //убираем свою фигуру
                    .setPiece(move.getRowIndexTo(), move.getColumnIndexTo(), pieceFrom);    //убираем свою фигуру на новое место (не зависимо от того, занята была ячейка или нет)
        }

        /**
         * Рокировка
         * Условие: король передвинулся на две клетки
         */
        private Builder executeCastling(Move move, Piece king) {
            int kingColumnIndexFrom = move.getColumnIndexFrom();
            int rookColumnIndexFrom;
            int rookColumnIndexTo;

            if (move.isLongCastling()) {
                rookColumnIndexFrom = ROOK_LONG_COLUMN_INDEX;
                rookColumnIndexTo = kingColumnIndexFrom + 1;
            } else {
                rookColumnIndexFrom = ROOK_SHORT_COLUMN_INDEX;
                rookColumnIndexTo = kingColumnIndexFrom - 1;
            }

            Piece rook = getCell(move.getRowIndexFrom(), rookColumnIndexFrom).getPiece();

            return cutPiece(move.getRowIndexFrom(), rookColumnIndexFrom)        //убираем ладью
                    .setPiece(move.getRowIndexFrom(), rookColumnIndexTo, rook)  //ставим ладью на новое место
                    .executeSimpleMove(move, king);                             //перемещаем короля
        }

        /**
         * Взятие на проходе
         * Условие: пешка покинула свою вертикаль, но при этом встала на пустую клетку
         */
        private Builder executeEnPassant(Move move, Piece pieceFrom) {
            return cutPiece(move.getRowIndexFrom(), move.getColumnIndexTo())    //убираем вражескую пешку взятую на проходе
                    .executeSimpleMove(move, pieceFrom);                        //перемещаем свою пешку
        }

        private boolean isCastling(Move move, Piece pieceFrom) {
            return pieceFrom.isKing() && move.isCastling();
        }

        private boolean isEnPassant(Move move, Piece pieceFrom) {
            if (pieceFrom.isPawn() && move.isPawnAttacks()) {
                return getCell(move.getRowIndexFrom(), move.getColumnIndexTo()) != null;
            }
            return false;
        }

        private boolean isPawnTransformation(Move move, Piece pieceFrom) {
            return pieceFrom.isPawn() && (move.getRowIndexTo() == 0 || move.getRowIndexTo() == 7);
        }

        private Builder setPiece(int rowIndex, int columnIndex, Piece piece) {
            CellDTO cell = getCell(rowIndex, columnIndex);
            CellsMatrix.this.cellsMatrix.get(rowIndex).set(columnIndex, cell.switchPiece(piece));
            return this;
        }

        private Builder cutPiece(int rowIndex, int columnIndex) {
            return setPiece(rowIndex, columnIndex, null);
        }

        public CellsMatrix build() {
            return CellsMatrix.this;
        }
    }

    /**
     * convert matrix (List<List<T>> x8x8) to simple list (List<T> x64)
     */
    public Stream<CellDTO> allPiecesStream() {
        if (Debug.IS_PARALLEL) {
            return cellsMatrix.parallelStream().flatMap(List::stream);
        } else {
            return cellsMatrix.stream().flatMap(List::stream);
        }
    }

    public Stream<CellDTO> allPiecesBySideStream(Side side) {
        return allPiecesStream()
                .filter(containsSide(side));
    }

    public Stream<CellDTO> includePiecesStream(Side side, PieceType... pieceTypes) {
        return allPiecesStream()
                .filter(containsPiecesOptimized(side, pieceTypes));
    }

    public Stream<CellDTO> excludePiecesStream(Side side, PieceType... pieceTypes) {
        return allPiecesStream()
                .filter(notContainsPiecesOptimized(side, pieceTypes));
    }

    private Predicate<CellDTO> containsPieces(Side side, PieceType[] pieceTypes) {
        return cell -> cell.getSide() == side && Arrays.stream(pieceTypes).anyMatch(type -> type == cell.getPieceType());
    }

    private Predicate<CellDTO> containsPiecesOptimized(Side side, PieceType[] pieceTypes) {
        return cell -> {
            if (cell.getSide() != side) {
                return false;
            }

            boolean result = false;
            for (PieceType pieceType : pieceTypes) {
                if (cell.getPieceType() == pieceType) {
                    result = true;
                }
            }
            return result;
        };
    }

    private Predicate<CellDTO> notContainsPiecesOptimized(Side side, PieceType[] pieceTypes) {
        return cell -> {
            if (cell.getSide() != side) {
                return false;
            }

            for (PieceType pieceType : pieceTypes) {
                if (cell.getPieceType() == pieceType) {
                    return false;
                }
            }
            return true;
        };
    }

    private Predicate<CellDTO> containsSide(Side side) {
        return cell -> cell.getSide() == side;
    }

    public Set<PointDTO> findPiecesCoords(Side side, PieceType... pieceTypes) {
        return includePiecesStream(side, pieceTypes)
                .map(CellDTO::getPoint)
                .collect(Collectors.toSet());
    }

    public PointDTO getKingPoint(Side side) throws KingNotFoundException {
        PointDTO kingPoint = kingPoints.get(side);
        if (kingPoint == null) {
            throw new KingNotFoundException(this, side);
        }

        return kingPoint;
    }

    public void print() {
        for (int i = 7; i >= 0; i--) {
            List<CellDTO> rows = cellsMatrix.get(i);
            for (int j = 7; j >= 0; j--) {
                System.out.print(cellToStr(rows.get(j)) + "\t");
            }
            System.out.println();
        }
    }

    private String cellToStr(CellDTO cell) {
        return cell.getPoint() + ": " + CommonUtils.getPieceName(cell.getPieceType(), true) + sideToStr(cell.getSide());
    }

    private String sideToStr(Side side) {
        if (side == null) {
            return "( )";
        } else if (side == Side.WHITE) {
            return "(W)";
        } else {
            return "(B)";
        }
    }
}
