package com.example.chess.service.support;

import com.example.chess.Debug;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"ConstantConditions", "Duplicates"})
public class OptimizedMoveHelper {

    private final FakeGame fakeGame;
    private final CellsMatrix originalMatrix;

    private OptimizedMoveHelper(FakeGame fakeGame, CellsMatrix originalMatrix) {
        this.fakeGame = fakeGame;
        this.originalMatrix = originalMatrix;
    }

    public static OptimizedMoveHelper valueOf(FakeGame fakeGame, CellsMatrix originalMatrix) {
        Debug.incrementMoveHelpersCount();
        return new OptimizedMoveHelper(fakeGame, originalMatrix);
    }

    public static void main(String[] args) {
        Stream<ExtendedMove> standardMovesStream = OptimizedMoveHelper.valueOf(null, null).getStandardMovesStream(Side.WHITE);
        boolean kingUnderAttack = OptimizedMoveHelper.valueOf(null, null).isKingUnderAttack(Side.WHITE);
    }

//    public boolean isKingUnderAttack(Side kingSide) {
//        PointDTO kingPoint = originalMatrix.findKingPoint(kingSide);
//        Set<PointDTO> enemyMoves = getUnfilteredPiecesMoves(fakeGame, originalMatrix, kingSide.reverse(), PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN);
//        return enemyMoves.contains(kingPoint);
//    }

    public Stream<ExtendedMove> getStandardMovesStream(Side side) {
        Side enemySide = side.reverse();
        PointDTO kingPoint = originalMatrix.findKingPoint(side);
        Set<PointDTO> enemyPoints = getAttackedByEnemyPoints(enemySide);

        List<PointDTO> unmovablePoints = originalMatrix
                .includePiecesStream(enemySide, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN)
                .map(enemyPossibleAttackerCell -> {
                    CellDTO unmovableCell = null;

                    switch (enemyPossibleAttackerCell.getPieceType()) {
                        case BISHOP:
                            unmovableCell = getSingleBishopDefenderCell(kingPoint, enemyPossibleAttackerCell);
                            break;
                        case ROOK:
                            unmovableCell = getSingleRookDefenderCell(kingPoint, enemyPossibleAttackerCell);
                            break;
                        case QUEEN:
                            unmovableCell = getSingleBishopDefenderCell(kingPoint, enemyPossibleAttackerCell);
                            if (unmovableCell == null) {
                                unmovableCell = getSingleRookDefenderCell(kingPoint, enemyPossibleAttackerCell);
                            }
                            break;
                    }

                    if (unmovableCell.getPieceType() == PieceType.QUEEN || unmovableCell.getPieceType() == enemyPossibleAttackerCell.getPieceType()) {
                        switch (unmovableCell.getPieceType()) {
                            case BISHOP:
                                break;
                            case ROOK:
                                break;
                            case QUEEN:
                                break;
                        }
                    }

                    return unmovableCell;
                })
                .filter(Objects::nonNull)
                .map(CellDTO::getPoint)
                .collect(Collectors.toList());


        return originalMatrix
                .allPiecesBySideStream(side)
                .flatMap(moveableCell -> {
                    Set<PointDTO> filteredMoves = getFilteredMovesForCell(moveableCell, enemyPoints, unmovablePoints);
                    return filteredMoves.stream().map(pointTo -> new ExtendedMove(moveableCell, originalMatrix.getCell(pointTo)));
                });
    }

    private CellDTO getSingleRookDefenderCell(PointDTO kingPoint, CellDTO cell) {
        int diff;
        int rowVector = 0;
        int columnVector = 0;

        if (kingPoint.getRowIndex() == cell.getRowIndex()) {
            diff = kingPoint.getColumnIndex() - cell.getColumnIndex();
            columnVector = diff > 0 ? 1 : -1;

        } else if (kingPoint.getColumnIndex() == cell.getColumnIndex()) {
            diff = kingPoint.getRowIndex() - cell.getRowIndex();
            rowVector = diff > 0 ? 1 : -1;
        } else {
            return null;
        }

        int rayLength = Math.abs(diff) - 1;
        return getNotEmptyBetweenCellIfSingle(kingPoint, rayLength, rowVector, columnVector);
    }

    private CellDTO getSingleBishopDefenderCell(PointDTO kingPoint, CellDTO cell) {
        int rowDiff = kingPoint.getRowIndex() - cell.getRowIndex();
        int columnDiff = kingPoint.getColumnIndex() - cell.getColumnIndex();

        if (Math.abs(rowDiff) == Math.abs(columnDiff)) {
            int rayLength = Math.abs(rowDiff) - 1;

            int rowVector = rowDiff > 0 ? 1 : -1;
            int columnVector = columnDiff > 0 ? 1 : -1;

            return getNotEmptyBetweenCellIfSingle(kingPoint, rayLength, rowVector, columnVector);
        }

        return null;
    }

    private CellDTO getNotEmptyBetweenCellIfSingle(PointDTO kingPoint, int rayLength, int rowVector, int columnVector) {
        int piecesBetweenCount = 0;
        CellDTO result = null;

        for (int i = 1; i <= rayLength; i++) {
            int rowOffset = i * rowVector;
            int columnOffset = i * columnVector;

            CellDTO betweenCell = getCellByKingPoint(kingPoint, rowOffset, columnOffset);
            if (!betweenCell.isEmpty()) {
                piecesBetweenCount++;
                if (result == null) {
                    result = betweenCell;
                }
            }
        }

        if (piecesBetweenCount == 1) {
            return result;
        }
        return null;
    }

    private Set<PointDTO> getAttackedByEnemyPoints(Side enemySide) {
        Set<PointDTO> result = new HashSet<>();
        originalMatrix
                .allPiecesBySideStream(enemySide)
                .forEach(movableCell -> {
                    if (movableCell.getPieceType() == PieceType.PAWN) {
                        addPawnDiagonalMoves(result, movableCell, enemySide);
                    } else {
                        addUnfilteredMovesForCell(result, movableCell);
                    }
                });
        return result;
    }

    private void addPawnDiagonalMoves(Set<PointDTO> moves, CellDTO movableCell, Side enemySide) {
        PointDTO point = movableCell.getPoint();
        int vector = enemySide.getPawnMoveVector();

        if (PointDTO.isCorrectIndex(point.getColumnIndex() + 1)) {
            moves.add(PointDTO.valueOf(point.getRowIndex() + vector, point.getColumnIndex() + 1));
        }
        if (PointDTO.isCorrectIndex(point.getColumnIndex() - 1)) {
            moves.add(PointDTO.valueOf(point.getRowIndex() + vector, point.getColumnIndex() - 1));
        }
    }

    /*
     * TODO: слабое место в производительности - предлагаю фильровать на стадии вычисления (т.е. не добавлять их, если нельзя)
     * 1) не ходим королем под атакуемые поля:
     * - вычисляем доступные ходы противника(кроме пешек)
     * - если доступное для короля поле является атакуемым - НЕ ДОБАВЛЯЕМ
     * 2) бросаем от короля лучи по всем 8 направлениям:
     * - если на горизонтально-вертикальных направлениях присутствует вражеская ладья или ферзь
     * - либо на диагональных направлениях присутствует вражеские слоны или ферзь
     *  - считаем сколько фигур между ними стоит
     *   - 0 не может - иначе был бы шах
     *   - 2+ - значит можно ходить любой из этих 2+
     *   - 1 - значит ЕЙ НЕЛЬЗЯ ХОДИТЬ
     */
//    private Set<PointDTO> filterAvailableMoves(Set<PointDTO> moves, CellDTO movableCell) {
//        moves = moves.stream()
//                .filter(isKingNotAttackedByEnemy(movableCell)
//                        .and(isKingNotAttackedByEnemyPawns(movableCell)))
//                .collect(Collectors.toSet());
//
//
//        //TODO: can optimize
//        if (movableCell.getPieceType() == PieceType.KING) {
//            Set<PointDTO> unavailableCastlingMoves = getUnavailableCastlingPoints(movableCell.getPoint(), moves);
//            moves.removeAll(unavailableCastlingMoves);            //запрещаем рокировку если король пересекает битое поле
//        }
//
//        return moves;
//    }
//
    private Set<PointDTO> getFilteredMovesForCell(CellDTO moveableCell, Set<PointDTO> enemyPoints, List<PointDTO> unmovablePoints) {
        Set<PointDTO> moves = new HashSet<>();
        addFilteredMovesForCell(moves, moveableCell, enemyPoints, unmovablePoints);
        return moves;
    }

    private void addUnfilteredMovesForCell(Set<PointDTO> moves, CellDTO moveableCell) {
        addMovesForCell(moves, moveableCell, null, null);
    }

    private void addFilteredMovesForCell(Set<PointDTO> moves, CellDTO moveableCell, Set<PointDTO> enemyPoints, List<PointDTO> unmovablePoints) {
        addMovesForCell(moves, moveableCell, Objects.requireNonNull(enemyPoints), Objects.requireNonNull(unmovablePoints));
    }

    /**
     * Не вызывай этот метод ниоткуда, кроме addUnfilteredMovesForCell()/addFilteredMovesForCell(), ладно?
     */
    private void addMovesForCell(Set<PointDTO> moves, CellDTO moveableCell, Set<PointDTO> enemyPoints, List<PointDTO> unmovablePoints) {
        new InternalMoveHelper(moves, moveableCell, enemyPoints, unmovablePoints)
                .addAnyPieceMoves();

        //FIXME: moves.size not actual
        Debug.incrementAvailablePointsFound(moves.size());
        Debug.incrementGetUnfilteredMovesCallsCount();
    }

    @SuppressWarnings({"PointlessArithmeticExpression"})
    private class InternalMoveHelper {

        private final CellDTO movableCell;
        private final boolean checkFilterEnabled;
        private final Set<PointDTO> enemyPoints;
        private final List<PointDTO> unmovablePoints;
        private final Set<PointDTO> moves;
        private final Side allySide;

        private InternalMoveHelper(Set<PointDTO> moves, CellDTO movableCell, Set<PointDTO> enemyPoints, List<PointDTO> unmovablePoints) {
            this.unmovablePoints = unmovablePoints;
            movableCell.requireNotEmpty();

            this.movableCell = movableCell;
            this.enemyPoints = enemyPoints;
            this.checkFilterEnabled = enemyPoints != null;
            this.moves = moves;
            this.allySide = movableCell.getSide();
        }

        private void addAnyPieceMoves() {
            if (checkFilterEnabled && unmovablePoints.contains(movableCell.getPoint())) {
                //TODO: на самом деле можно ходить по той же диагонали/горизонтали
                //поэтому List<PointDTO> -> to Map<PointDTO unmovablePoint, List<PointDTO> availablePoints>
                return;
            }

            switch (movableCell.getPieceType()) {
                case PAWN:
                    addMovesForPawn();
                    break;
                case KNIGHT:
                    addMovesForKnight();
                    break;
                case BISHOP:
                    addMovesForBishop();
                    break;
                case ROOK:
                    addMovesForRook();
                    break;
                case QUEEN:
                    addMovesForQueen();
                    break;
                case KING:
                    addMovesForKing();
                    break;
            }
        }

        private void addMovesForKnight() {
            addMove(1, 2);
            addMove(2, 1);
            addMove(1, -2);
            addMove(2, -1);
            addMove(-1, 2);
            addMove(-2, 1);
            addMove(-1, -2);
            addMove(-2, -1);
        }

        private void addMovesForBishop() {
            addMovesByVector(1, 1);
            addMovesByVector(-1, 1);
            addMovesByVector(1, -1);
            addMovesByVector(-1, -1);
        }

        private void addMovesForRook() {
            addMovesByVector(1, 0);
            addMovesByVector(-1, 0);
            addMovesByVector(0, 1);
            addMovesByVector(0, -1);
        }

        private void addMovesForQueen() {
            addMovesForRook();
            addMovesForBishop();
        }

        private void addMovesForKing() {
            addKingMove(0, 1);
            addKingMove(0, -1);
            addKingMove(1, 0);
            addKingMove(-1, 0);

            addKingMove(1, 1);
            addKingMove(1, -1);
            addKingMove(-1, 1);
            addKingMove(-1, -1);

            Integer kingRowIndex = movableCell.getRowIndex();

            if (fakeGame.isShortCastlingAvailable(allySide)) {
                if (isEmptyCellsOnRow(kingRowIndex, 1, 2)) {
                    if (isSafeCrossPointForCastling(-1)) {
                        addKingMove(0, -2);
                    }
                }
            }
            if (fakeGame.isLongCastlingAvailable(allySide)) {
                if (isEmptyCellsOnRow(kingRowIndex, 4, 5, 6)) {
                    if (isSafeCrossPointForCastling(1)) {
                        addKingMove(0, 2);
                    }
                }
            }
        }

        private void addMovesForPawn() {
            int currentRow = movableCell.getRowIndex();
            int currentColumn = movableCell.getColumnIndex();
            int vector = allySide.getPawnMoveVector();
            Side enemySide = allySide.reverse();

            boolean isAdded = addMove(1 * vector, 0);
            if (isAdded) {
                boolean isFirstMove = currentRow == 1 || currentRow == 6;
                if (isFirstMove) {
                    addMove(2 * vector, 0);
                }
            }

            addPawnAttackMoves(enemySide, vector);

            CellDTO cell = getCellByOffsets(1 * vector, 1);
            if (cell.getSide() == enemySide) {
                //attack
                moves.add(cell.getPoint());
            }

            cell = getCellByOffsets(1 * vector, -1);
            if (cell.getSide() == enemySide) {
                //attack
                moves.add(cell.getPoint());
            }

            Integer enemyLongMoveColumnIndex = fakeGame.getPawnLongMoveColumnIndex(enemySide);
            if (enemyLongMoveColumnIndex != null) {    //противник только что сделал длинный ход пешкой

                if (Math.abs(enemyLongMoveColumnIndex - currentColumn) == 1) {    //и эта пешка рядом с выделенной (слева или справа)

                    if (currentRow == 3 || currentRow == 4) {        //и это творится на нужной горизонтали
                        //значит можно делать взятие на проходе
                        //проверять ничего не нужно, эта ячейка 100% пуста (не могла же пешка перепрыгнуть фигуру)
                        //noinspection ConstantConditions
                        addMove(1 * vector, enemyLongMoveColumnIndex - currentColumn);
                    }
                }
            }
        }

        /**
         * @return true if move added, false - otherwise (addable cell is null or ally)
         */
        private boolean addMove(int rowIndexOffset, int columnIndexOffset) {
            CellDTO cell = getCellByOffsets(rowIndexOffset, columnIndexOffset);
            if (cell != null && cell.getSide() != allySide) {
                moves.add(cell.getPoint());
                return true;
            }
            return false;
        }

        private void addMovesByVector(int rowVector, int columnVector) {
            for (int i = 1; i < 8; i++) {
                boolean isAdded = addMove(rowVector * i, columnVector * i);
                if (!isAdded) {
                    return;
                }
            }
        }

        private void addKingMove(int rowIndexOffset, int columnIndexOffset) {
            CellDTO cell = getCellByOffsets(rowIndexOffset, columnIndexOffset);
            if (cell != null && cell.getSide() != allySide) {
                PointDTO point = cell.getPoint();

                if (checkFilterEnabled) {
                    if (!enemyPoints.contains(point)) {
                        moves.add(point);
                    }
                } else {
                    moves.add(point);
                }
            }
        }

        private boolean isSafeCrossPointForCastling(int columnIndexOffset) {
            PointDTO crossPoint = getPointByOffsets(0, columnIndexOffset);
            if (crossPoint != null) {
                return checkFilterEnabled && !enemyPoints.contains(crossPoint);
            }
            return false;
        }

        private void addPawnAttackMoves(Side enemySide, int vector) {
            CellDTO cell = getCellByOffsets(1 * vector, 1);
            if (cell != null && cell.getSide() == enemySide) {
                moves.add(cell.getPoint());
            }

            cell = getCellByOffsets(1 * vector, -1);
            if (cell != null && cell.getSide() == enemySide) {
                moves.add(cell.getPoint());
            }
        }

        private boolean isEmptyCellsOnRow(int rowIndex, int... columnIndexes) {
            for (int columnIndex : columnIndexes) {
                if (!isEmptyCell(rowIndex, columnIndex)) {
                    return false;
                }
            }

            return true;
        }


        private boolean isEmptyCell(int rowIndex, int columnIndex) {
            CellDTO cell = getCell(rowIndex, columnIndex);
            Objects.requireNonNull(cell);
            return Objects.requireNonNull(cell).isEmpty();
        }


        private CellDTO getCellByOffsets(int rowIndexOffset, int columnIndexOffset) {
            return getCell(movableCell.getRowIndex() + rowIndexOffset, movableCell.getColumnIndex() + columnIndexOffset);
        }

        private PointDTO getPointByOffsets(int rowIndexOffset, int columnIndexOffset) {
            CellDTO cell = getCellByOffsets(rowIndexOffset, columnIndexOffset);
            if (cell != null) {
                return cell.getPoint();
            }
            return null;
        }
    }

    private CellDTO getCell(int rowIndex, int columnIndex) {
        if (PointDTO.isCorrectIndex(rowIndex, columnIndex)) {
            return originalMatrix.getCell(rowIndex, columnIndex);
        }
        return null;
    }

    private CellDTO getCellByKingPoint(PointDTO kingPoint, int rowIndexOffset, int columnIndexOffset) {
        return getCell(kingPoint.getRowIndex() + rowIndexOffset, kingPoint.getColumnIndex() + columnIndexOffset);
    }
}
