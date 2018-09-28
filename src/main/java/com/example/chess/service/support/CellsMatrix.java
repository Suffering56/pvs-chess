package com.example.chess.service.support;

import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.History;
import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.utils.MoveResult;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.chess.ChessConstants.BOARD_SIZE;
import static com.example.chess.ChessConstants.ROOK_LONG_COLUMN_INDEX;
import static com.example.chess.ChessConstants.ROOK_SHORT_COLUMN_INDEX;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class CellsMatrix implements Iterable<List<CellDTO>> {

    @Getter
    private final int position;
    private final List<List<CellDTO>> cellsMatrix;

    private CellsMatrix(int position, Function<Integer, Function<Integer, Piece>> cellGenerator) {
        this.position = position;
        this.cellsMatrix = new ArrayList<>(BOARD_SIZE);

        //1-8
        for (int rowIndex = 0; rowIndex < BOARD_SIZE; rowIndex++) {
            List<CellDTO> rowCells = new ArrayList<>(BOARD_SIZE);

            //A-H
            for (int columnIndex = 0; columnIndex < BOARD_SIZE; columnIndex++) {
                Piece piece = cellGenerator.apply(rowIndex).apply(columnIndex);
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

    private void checkPoint(int rowIndex, int columnIndex) {
        Preconditions.checkElementIndex(rowIndex, BOARD_SIZE, "Out of board point");
        Preconditions.checkElementIndex(columnIndex, BOARD_SIZE, "Out of board point");
    }

    /**
     * Обычный ход
     *
     * @return MoveResult (contains new matrix with updated state)
     * description:
     * - Перемещаем фигуру из положения #from(X.Y) в положение #to(X.Y).
     * - Убираем фигуру из #from.
     * - В #to ставим ту фигуру, которой ходим (кроме случаев превращения пешки)
     * <p>
     * Условие: это не рокировка и не взятие на проходе.
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

        return new MoveResult(this, newMatrix);
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

        return new MoveResult(this, newMatrix);
    }

    /**
     * Взятие на проходе
     * Условие: пешка покинула свою вертикаль, но при этом ничего не срубила
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

        return new MoveResult(this, newMatrix);
    }

    public ArrangementDTO generateArrangement(Side underCheckSide) {
        return new ArrangementDTO(position, cellsMatrix, underCheckSide);
    }

    public static Builder builder(List<History> historyList, int position) {
        return CellsMatrix.ofHistory(historyList, position).new Builder();
    }

    public static Builder builder(CellsMatrix prevMatrix, int position) {
        return CellsMatrix.ofOtherMatrix(prevMatrix, position).new Builder();
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

    private static CellsMatrix ofOtherMatrix(CellsMatrix prevMatrix, int position) {
        return new CellsMatrix(position, rowIndex -> columnIndex -> {
            CellDTO cell = prevMatrix.getCell(rowIndex, columnIndex);
            return cell.getPiece();
        });
    }

    @Override
    public Iterator<List<CellDTO>> iterator() {
        return cellsMatrix.iterator();
    }

    @Override
    public void forEach(Consumer<? super List<CellDTO>> action) {
        cellsMatrix.forEach(action);
    }

    @Override
    public Spliterator<List<CellDTO>> spliterator() {
        return cellsMatrix.spliterator();
    }

    //TODO: стримы поидее не должны нарушать иммутабельность класса
    public Stream<List<CellDTO>> stream() {
        return cellsMatrix.stream();
    }

    public List<History> generateHistory(Long gameId, int position) {
        return cellsMatrix.stream()
                .flatMap(List::stream)                        //convert matrix (List<List<T>> x8x8) to simple list (List<T> x64)
                .filter(cell -> cell.getPiece() != null)
                .map(cell -> History.ofCell(cell, gameId, position))
                .collect(Collectors.toList());
    }

    public Stream<CellDTO> filteredPiecesStream(Side side, PieceType... pieceTypes) {
        return allPiecesStream()
                .filter(containsPieces(side, pieceTypes));
    }

    public Stream<CellDTO> allPiecesStream() {
        return cellsMatrix.stream()
                .flatMap(List::stream);
    }

    public Set<PointDTO> findPiecesCoords(Side side, PieceType... pieceTypes) {
        return filteredPiecesStream(side, pieceTypes)
                .map(CellDTO::getPoint)
                .collect(Collectors.toSet());
    }

    private Predicate<CellDTO> containsPieces(Side side, PieceType[] pieceTypes) {
        return cell -> cell.getPieceSide() == side && Arrays.stream(pieceTypes).anyMatch(type -> type == cell.getPieceType());
    }
}
