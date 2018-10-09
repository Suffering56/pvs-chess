package com.example.chess.service.support;

import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.History;
import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.chess.ChessConstants.BOARD_SIZE;
import static com.example.chess.ChessConstants.ROOK_LONG_COLUMN_INDEX;
import static com.example.chess.ChessConstants.ROOK_SHORT_COLUMN_INDEX;

public final class CellsMatrix implements Immutable{

    @Getter
    private final int position;
    private final List<List<CellDTO>> cellsMatrix;

    private CellsMatrix(int position, Function<Integer, Function<Integer, Piece>> pieceGenerator) {
        this.position = position;
        this.cellsMatrix = new ArrayList<>(BOARD_SIZE);

        //1-8
        for (int rowIndex = 0; rowIndex < BOARD_SIZE; rowIndex++) {
            List<CellDTO> rowCells = new ArrayList<>(BOARD_SIZE);

            //A-H
            for (int columnIndex = 0; columnIndex < BOARD_SIZE; columnIndex++) {
                Piece piece = pieceGenerator.apply(rowIndex).apply(columnIndex);
                CellDTO cell = CellDTO.valueOf(rowIndex, columnIndex, piece);
                rowCells.add(cell);
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

    public CellDTO getCell(History historyItem) {
        return getCell(historyItem.getRowIndex(), historyItem.getColumnIndex());
    }

    public boolean isEmptyCell(int rowIndex, int columnIndex) {
        return getCell(rowIndex, columnIndex).isEmpty();
    }

    private void checkPoint(int rowIndex, int columnIndex) {
        Preconditions.checkElementIndex(rowIndex, BOARD_SIZE, "Out of board point");
        Preconditions.checkElementIndex(columnIndex, BOARD_SIZE, "Out of board point");
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
    public MoveResult executeMove(MoveDTO move, Piece pieceFromPawn) {
        CellDTO cellFrom = getCell(move.getFrom());
        CellDTO cellTo = getCell(move.getTo());

        Piece pieceTo = cellFrom.getPiece();
        if (pieceFromPawn != null) {
            pieceTo = pieceFromPawn;
        }

        CellsMatrix newMatrix = CellsMatrix
                .builder(this, position + 1)
                //move any piece
                .setPiece(cellTo.getPoint(), pieceTo)
                .cutDownPiece(cellFrom.getPoint())
                .build();

        return MoveResult.valueOf(this, newMatrix);
    }

    /**
     * Рокировка
     * Условие: король передвинулся на две клетки
     */
    public MoveResult executeCastling(MoveDTO move) {
        CellDTO kingCellFrom = getCell(move.getFrom());
        CellDTO kingCellTo = getCell(move.getTo());

        int rookRowIndex = move.getFrom().getRowIndex();
        Integer kingColumnIndex = move.getFrom().getColumnIndex();

        CellDTO rookCellFrom;
        int rookColumnIndexTo;

        if (move.isShortCastling()) {
            rookCellFrom = getCell(rookRowIndex, ROOK_SHORT_COLUMN_INDEX);
            rookColumnIndexTo = kingColumnIndex - 1;
        } else {
            rookCellFrom = getCell(rookRowIndex, ROOK_LONG_COLUMN_INDEX);
            rookColumnIndexTo = kingColumnIndex + 1;
        }

        CellsMatrix newMatrix = CellsMatrix
                .builder(this, position + 1)
                //move king
                .setPiece(kingCellTo.getPoint(), kingCellFrom.getPiece())
                .cutDownPiece(kingCellFrom.getPoint())
                //move rook
                .setPiece(rookRowIndex, rookColumnIndexTo, rookCellFrom.getPiece())
                .cutDownPiece(rookCellFrom.getPoint())
                .build();

        return MoveResult.valueOf(this, newMatrix);
    }

    /**
     * Взятие на проходе
     * Условие: пешка покинула свою вертикаль, но при этом встала на пустую клетку
     */
    public MoveResult executeEnPassant(MoveDTO move) {
        CellDTO cellFrom = getCell(move.getFrom());
        CellDTO cellTo = getCell(move.getTo());

        CellsMatrix newMatrix = CellsMatrix
                .builder(this, position + 1)
                //move pawn
                .setPiece(cellTo.getPoint(), cellFrom.getPiece())
                .cutDownPiece(cellFrom.getPoint())
                //cut down enemy pawn
                .cutDownPiece(move.getFrom().getRowIndex(), move.getTo().getColumnIndex())
                .build();

        return MoveResult.valueOf(this, newMatrix);
    }

    public ArrangementDTO generateArrangement(Side underCheckSide) {
        return new ArrangementDTO(position, cellsMatrix, underCheckSide);
    }

    public static Builder builder(List<History> historyList, int position) {
        return CellsMatrix.ofHistory(historyList, position).new Builder();
    }

    private static CellsMatrix ofHistory(List<History> historyList, int position) {
        Map<Integer, Map<Integer, Piece>> historyMap = historyList
                .stream()
                .collect(Collectors.groupingBy(History::getRowIndex,
                        Collectors.toMap(History::getColumnIndex, History::getPiece)));

        return new CellsMatrix(position, rowIndex -> columnIndex -> {
            Map<Integer, Piece> subMap = historyMap.get(rowIndex);
            if (subMap != null) {
                return subMap.get(columnIndex);
            }
            return null;
        });
    }

    public static Builder builder(CellsMatrix prevMatrix, int position) {
        return CellsMatrix.ofOtherMatrix(prevMatrix, position).new Builder();
    }

    private static CellsMatrix ofOtherMatrix(CellsMatrix prevMatrix, int position) {
        return new CellsMatrix(position, rowIndex -> columnIndex -> {
            CellDTO cell = prevMatrix.getCell(rowIndex, columnIndex);
            return cell.getPiece();
        });
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public class Builder {

        public Builder setPiece(int rowIndex, int columnIndex, Piece piece) {
            CellDTO cell = getCell(rowIndex, columnIndex);
            CellsMatrix.this.cellsMatrix.get(rowIndex).set(columnIndex, cell.switchPiece(piece));
            return this;
        }

        public Builder setPiece(PointDTO point, Piece piece) {
            return setPiece(point.getRowIndex(), point.getColumnIndex(), piece);
        }

        public Builder cutDownPiece(int rowIndex, int columnIndex) {
            return setPiece(rowIndex, columnIndex, null);
        }

        public Builder cutDownPiece(PointDTO point) {
            return setPiece(point, null);
        }

        public CellsMatrix build() {
            return CellsMatrix.this;
        }
    }

    /**
     * convert matrix (List<List<T>> x8x8) to simple list (List<T> x64)
     */
    public Stream<CellDTO> allPiecesStream() {
        return cellsMatrix.stream().flatMap(List::stream);
    }

    public Stream<CellDTO> allPiecesBySideStream(Side side) {
        return allPiecesStream()
                .filter(containsSide(side));
    }

    public Stream<CellDTO> somePiecesStream(Side side, PieceType... pieceTypes) {
        return allPiecesStream()
                .filter(containsPieces(side, pieceTypes));
    }

    private Predicate<CellDTO> containsPieces(Side side, PieceType[] pieceTypes) {
        return cell -> cell.getSide() == side && Arrays.stream(pieceTypes).anyMatch(type -> type == cell.getPieceType());
    }

    private Predicate<CellDTO> containsSide(Side side) {
        return cell -> cell.getSide() == side;
    }

    public List<History> generateHistory(Long gameId, int position) {
        return allPiecesStream()
                .filter(cell -> cell.getPiece() != null)
                .map(cell -> History.ofCell(cell, gameId, position))
                .collect(Collectors.toList());
    }

    public Set<PointDTO> findPiecesCoords(Side side, PieceType... pieceTypes) {
        return somePiecesStream(side, pieceTypes)
                .map(CellDTO::getPoint)
                .collect(Collectors.toSet());
    }

    public PointDTO findKingPoint(Side side) {
        return somePiecesStream(side, PieceType.KING)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("KING not found on board"))
                .getPoint();
    }
}
