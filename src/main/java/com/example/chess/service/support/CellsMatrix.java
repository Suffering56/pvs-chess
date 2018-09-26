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
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.chess.ChessConstants.BOARD_SIZE;

//TODO: make me immutable
@SuppressWarnings("ALL")
@Log4j2
public class CellsMatrix implements Iterable<List<CellDTO>> {

    private List<List<CellDTO>> cellsMatrix;
    private int position;
    private Side underCheckSide;

    private CellsMatrix(int position, Side underCheckSide) {
        this.position = position;
        this.underCheckSide = underCheckSide;
        this.cellsMatrix = new ArrayList<>(BOARD_SIZE);

        //1-8
        for (int rowIndex = 0; rowIndex < BOARD_SIZE; rowIndex++) {
            List<CellDTO> rowCells = new ArrayList<>(BOARD_SIZE);

            //A-H
            for (int columnIndex = 0; columnIndex < BOARD_SIZE; columnIndex++) {
                rowCells.add(new CellDTO(rowIndex, columnIndex));
            }
            cellsMatrix.add(rowCells);
        }
    }

    public static CellsMatrix createByHistory(List<History> historyList, int position) {
        CellsMatrix instance = new CellsMatrix(position, null);

        for (History item : historyList) {
            CellDTO cell = instance.getCell(item);
            cell.setPiece(item.getPiece());
        }

        return instance;
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

    public static void checkPoint(int rowIndex, int columnIndex) {
        Preconditions.checkElementIndex(rowIndex, BOARD_SIZE, "Out of board point");
        Preconditions.checkElementIndex(columnIndex, BOARD_SIZE, "Out of board point");
    }

    public MoveResult executeMove(MoveDTO move) {
        return executeMove(move, null);
    }

    public MoveResult executeMove(MoveDTO move, Piece pieceFromPawn) {
        CellDTO cellFrom = getCell(move.getFrom());
        CellDTO cellTo = getCell(move.getTo());

        Piece pieceFrom = cellFrom.getPiece();
        Piece pieceTo = cellTo.getPiece();

        if (pieceFromPawn == null) {
            cellTo.setPiece(cellFrom.getPiece());
        } else {
            cellTo.setPiece(pieceFromPawn);
        }
        cellFrom.setPiece(null);

        return new MoveResult(cellFrom, cellTo, pieceFrom, pieceTo);
    }


    public List<History> generateHistory(Long gameId, int position) {
        return cellsMatrix.stream()
                .flatMap(List::stream)                        //convert matrix (List<List<T>> x8x8) to simple list (List<T> x64)
                .filter(cell -> cell.getPiece() != null)
                .map(cell -> History.createByCell(cell, gameId, position))
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
                .map(CellDTO::generatePoint)
                .collect(Collectors.toSet());
    }

    private Predicate<CellDTO> containsPieces(Side side, PieceType[] pieceTypes) {
        return cell -> cell.getPieceSide() == side && Arrays.stream(pieceTypes).anyMatch(type -> type == cell.getPieceType());
    }


    public Stream<List<CellDTO>> stream() {
        return cellsMatrix.stream();
    }

    public void print() {
        cellsMatrix.forEach(row -> row.forEach(CellDTO::print));
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

    public ArrangementDTO generateArrangement(Side underCheckSide) {
        return new ArrangementDTO(position, cellsMatrix, underCheckSide);
    }
}
