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
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.chess.ChessConstants.BOARD_SIZE;
import static com.example.chess.ChessConstants.ROOK_LONG_COLUMN_INDEX;
import static com.example.chess.ChessConstants.ROOK_SHORT_COLUMN_INDEX;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class CellsMatrix {

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

    public boolean isEmptyCell(int rowIndex, int columnIndex) {
        return getCell(rowIndex, columnIndex).isEmpty();
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

    public Stream<CellDTO> somePiecesStream(Side side, PieceType... pieceTypes) {
        return allPiecesStream()
                .filter(containsPieces(side, pieceTypes));
    }

    private Predicate<CellDTO> containsPieces(Side side, PieceType[] pieceTypes) {
        return cell -> cell.getSide() == side && Arrays.stream(pieceTypes).anyMatch(type -> type == cell.getPieceType());
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

//    public Set<PointDTO> getPiecesMoves(Side side, Function<CellDTO, Set<PointDTO>> availableMovesFinder, PieceType... pieceTypes) {
//        return somePiecesStream(side, pieceTypes)
//                .map(availableMovesFinder)
//                .flatMap(Set::stream)
//                .collect(Collectors.toSet());
//    }


//    private class MoveHelper {
//
////        private Set<PointDTO> getPiecesMoves(Game game, Side side, PieceType... pieceTypes) {
////            return  somePiecesStream(side, pieceTypes)
////                    .map(cellDTO -> getAvailableMovesForSingleCell(game, cellDTO))
////                    .flatMap(Set::stream)
////                    .collect(Collectors.toSet());
////        }
//
//        private Set<PointDTO> getAvailableMovesForSingleCell(Game game, CellDTO moveableCell) {
//            moveableCell.requireNotEmpty();
//            InternalMoveHelper helper = new InternalMoveHelper(game, moveableCell);
//            Set<PointDTO> moves;
//
//            //noinspection ConstantConditions
//            switch (moveableCell.getPieceType()) {
//                case PAWN: {
//                    moves = helper.getMovesForPawn();
//                    break;
//                }
//                case KNIGHT: {
//                    moves = helper.getMovesForKnight();
//                    break;
//                }
//                case BISHOP: {
//                    moves = helper.getMovesForBishop();
//                    break;
//                }
//                case ROOK: {
//                    moves = helper.getMovesForRook();
//                    break;
//                }
//                case QUEEN: {
//                    moves = helper.getMovesForQueen();
//                    break;
//                }
//                case KING: {
//                    moves = helper.getMovesForKing();
//                    break;
//                }
//                default:
//                    moves = new HashSet<>();
//            }
//            return moves;
//        }
//
//
//        private class InternalMoveHelper {
//
//            private final Game game;
//            private final CellDTO moveableCell;
//
//            public InternalMoveHelper(Game game, CellDTO moveableCell) {
//                this.game = game;
//                this.moveableCell = moveableCell;
//            }
//
//            private Set<PointDTO> getMovesForPawn() {
//                Set<PointDTO> moves = new HashSet<>();
//
//                int activeRow = moveableCell.getRowIndex();
//                int activeColumn = moveableCell.getColumnIndex();
//                Side allySide = moveableCell.getSide();
//
//                int vector = 1;
//                if (allySide == Side.BLACK) {
//                    vector = -1;
//                }
//
//                boolean isFirstMove = false;
//                if (activeRow == 1 || activeRow == 6) {
//                    isFirstMove = true;
//                }
//
//                @SuppressWarnings("PointlessArithmeticExpression")
//                CellDTO cell = getCell(activeRow + 1 * vector, activeColumn);
//                boolean isAdded = addPawnMove(moves, cell, allySide, false);
//
//                if (isFirstMove && isAdded) {
//                    cell = getCell(activeRow + 2 * vector, activeColumn);
//                    addPawnMove(moves, cell, allySide, false);
//                }
//
//                //attack
//                cell = getCell(activeRow + vector, activeColumn + 1);
//                addPawnMove(moves, cell, allySide, true);
//
//                cell = getCell(activeRow + vector, activeColumn - 1);
//                addPawnMove(moves, cell, allySide, true);
//
//                Integer enemyLongMoveColumnIndex = game.getPawnLongMoveColumnIndex(moveableCell.getEnemySide());
//                if (enemyLongMoveColumnIndex != null) {    //противник только что сделал длинный ход пешкой
//
//                    if (Math.abs(enemyLongMoveColumnIndex - activeColumn) == 1) {    //и эта пешка рядом с выделенной (слева или справа)
//
//                        if (activeRow == 3 || activeRow == 4) {        //и это творится на нужной горизонтали
//                            //значит можно делать взятие на проходе
//                            //проверять ничего не нужно, эта ячейка 100% пуста (не могла же пешка перепрыгнуть фигуру)
//                            //noinspection ConstantConditions
//                            moves.add(PointDTO.valueOf(activeRow + allySide.getPawnMoveVector(), enemyLongMoveColumnIndex));
//                        }
//                    }
//                }
//
//                return moves;
//            }
//
//            private Set<PointDTO> getMovesForKnight() {
//                Set<PointDTO> moves = new HashSet<>();
//
//                addKnightMove(moves, 1, 2);
//                addKnightMove(moves, 2, 1);
//                addKnightMove(moves, 1, -2);
//                addKnightMove(moves, 2, -1);
//                addKnightMove(moves, -1, 2);
//                addKnightMove(moves, -2, 1);
//                addKnightMove(moves, -1, -2);
//                addKnightMove(moves, -2, -1);
//
//                return moves;
//            }
//
//            private Set<PointDTO> getMovesForBishop() {
//                Set<PointDTO> moves = new HashSet<>();
//
//                addAvailableMovesForRay(moves, 1, 1);
//                addAvailableMovesForRay(moves, -1, 1);
//                addAvailableMovesForRay(moves, 1, -1);
//                addAvailableMovesForRay(moves, -1, -1);
//
//                return moves;
//            }
//
//            private Set<PointDTO> getMovesForRook() {
//                Set<PointDTO> moves = new HashSet<>();
//
//                addAvailableMovesForRay(moves, 1, 0);
//                addAvailableMovesForRay(moves, -1, 0);
//                addAvailableMovesForRay(moves, 0, 1);
//                addAvailableMovesForRay(moves, 0, -1);
//
//                return moves;
//            }
//
//            private Set<PointDTO> getMovesForQueen() {
//                Set<PointDTO> moves = new HashSet<>();
//
//                moves.addAll(getMovesForRook());
//                moves.addAll(getMovesForBishop());
//
//                return moves;
//            }
//
//            private Set<PointDTO> getMovesForKing() {
//                Set<PointDTO> moves = new HashSet<>();
//
//                addAvailableMovesForRay(moves, 1, 0, 1);
//                addAvailableMovesForRay(moves, -1, 0, 1);
//                addAvailableMovesForRay(moves, 0, 1, 1);
//                addAvailableMovesForRay(moves, 0, -1, 1);
//                addAvailableMovesForRay(moves, 1, 1, 1);
//                addAvailableMovesForRay(moves, -1, 1, 1);
//                addAvailableMovesForRay(moves, 1, -1, 1);
//                addAvailableMovesForRay(moves, -1, -1, 1);
//
//                if (game.isShortCastlingAvailable(moveableCell.getSide())) {
//                    if (isEmptyCellsByActiveRow(1, 2)) {
//                        addMove(moves, getCell(moveableCell.getRowIndex(), moveableCell.getColumnIndex() - 2), moveableCell.getSide());
//                    }
//                }
//                if (game.isLongCastlingAvailable(moveableCell.getSide())) {
//                    if (isEmptyCellsByActiveRow(4, 5, 6)) {
//                        addMove(moves, getCell(moveableCell.getRowIndex(), moveableCell.getColumnIndex() + 2), moveableCell.getSide());
//                    }
//                }
//
//                return moves.stream()
//                        //фильровать здесь!!!
//                        //если это делать там, где происходят остальные проверки на шах - попадем в бесконечную рекурсию
//                        .filter(isNotAttackingByEnemyKing(moveableCell.getEnemySide()))
//                        .collect(Collectors.toSet());
//            }
//
//            private void addAvailableMovesForRay(Set<PointDTO> moves, int rowVector, int columnVector) {
//                addAvailableMovesForRay(moves, rowVector, columnVector, 7);
//            }
//
//            private void addAvailableMovesForRay(Set<PointDTO> moves, int rowVector, int columnVector, int rayLength) {
//                for (int i = 1; i < rayLength + 1; i++) {
//                    CellDTO cell = getCell(moveableCell.getRowIndex() + rowVector * i, moveableCell.getColumnIndex() + columnVector * i);
//                    if (!addMove(moves, cell, moveableCell.getSide())) {
//                        break;
//                    }
//                }
//            }
//
//            private boolean addMove(Set<PointDTO> moves, CellDTO cell, Side allySide) {
//                if (cell == null || cell.getSide() == allySide) {
//                    //IndexOutOfBounds
//                    return false;
//                }
//
//                moves.add(cell.getPoint());
//                return cell.getSide() != allySide.reverse();
//            }
//
//            private void addKnightMove(Set<PointDTO> moves, int rowOffset, int columnOffset) {
//                CellDTO cell = getCell(moveableCell.getRowIndex() + rowOffset, moveableCell.getColumnIndex() + columnOffset);
//                if (cell != null && cell.getSide() != moveableCell.getSide()) {
//                    moves.add(cell.getPoint());
//                }
//            }
//
//            private boolean addPawnMove(Set<PointDTO> moves, CellDTO cell, Side allySide, boolean isAttack) {
//                if (cell == null || cell.getSide() == allySide) {
//                    //IndexOutOfBounds
//                    return false;
//                }
//
//                if (isAttack) {
//                    if (cell.getSide() == allySide.reverse()) {
//                        moves.add(cell.getPoint());
//                    }
//                    return false;
//                } else {
//                    if (cell.getSide() == allySide.reverse()) {
//                        return false;
//                    } else {
//                        moves.add(cell.getPoint());
//                        return true;
//                    }
//                }
//            }
//
//            /**
//             * Проверяет не встали ли мы королем под шах вражеского короля.
//             */
//            private Predicate<PointDTO> isNotAttackingByEnemyKing(Side enemySide) {
//                PointDTO enemyKingPoint = findKingPoint(enemySide);
//                return enemyKingPoint::isNotBorderedBy;  //или может подошли вплотную к вражескому королю?
//            }
//
//            private boolean isEmptyCellsByActiveRow(int... columnIndexes) {
//                for (int columnIndex : columnIndexes) {
//                    if (!isEmptyCell(moveableCell.getRowIndex(), columnIndex)) {
//                        return false;
//                    }
//                }
//
//                return true;
//            }
//        }
//    }


}
