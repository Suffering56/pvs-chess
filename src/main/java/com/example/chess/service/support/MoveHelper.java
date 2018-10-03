package com.example.chess.service.support;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.support.api.MoveHelperAPI;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoveHelper implements MoveHelperAPI {

    private final Game game;
    private final CellsMatrix originalMatrix;

    public MoveHelper(Game game, CellsMatrix originalMatrix) {
        this.game = game;
        this.originalMatrix = originalMatrix;
    }

    @Override
    public Set<PointDTO> getFilteredAvailableMoves(PointDTO pointFrom) {
        CellDTO moveableCell = originalMatrix.getCell(pointFrom);
        return getFilteredAvailableMoves(moveableCell);
    }

    @Override
    public Set<PointDTO> getFilteredAvailableMoves(CellDTO moveableCell) {
        Set<PointDTO> moves = getUnfilteredAvailableMoves(game, originalMatrix, moveableCell, false);
        return filterAvailableMoves(moves, moveableCell);
    }

    @Override
    public boolean isKingUnderAttack(Side kingSide) {
        PointDTO kingPoint = originalMatrix.findKingPoint(kingSide);
        Set<PointDTO> enemyMoves = getUnfilteredPiecesMoves(game, originalMatrix, kingSide.reverse(), PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN);
        return enemyMoves.contains(kingPoint);
    }

    private List<ExtendedMove> getAvailableDefensiveMoves() {
        throw new UnsupportedOperationException();
    }

    private List<ExtendedCell> getAttackers(PointDTO threatenedPoint) {
        CellDTO threatenedCell = originalMatrix.getCell(threatenedPoint);

        List<ExtendedCell> collect = getExtendedCellStream(threatenedCell.getEnemySide())
                .filter(moves -> !moves.isEmpty() && moves.getAvailablePoints().contains(threatenedPoint))
                .collect(Collectors.toList());

        throw new UnsupportedOperationException();
    }

    private Set<PointDTO> filterAvailableMoves(Set<PointDTO> moves, CellDTO moveableCell) {
        moves = moves.stream()
                .filter(isKingNotAttackedByEnemy(moveableCell)
                        .and(isKingNotAttackedByEnemyPawns(moveableCell)))
                .collect(Collectors.toSet());


        //TODO: can optimize
        if (moveableCell.getPieceType() == PieceType.KING) {
            Set<PointDTO> unavailableCastlingMoves = getUnavailableCastlingPoints(moveableCell.getPoint(), moves);
            moves.removeAll(unavailableCastlingMoves);            //запрещаем рокировку если король пересекает битое поле
        }

        return moves;
    }

    private Set<PointDTO> getUnfilteredPiecesMoves(Game game, CellsMatrix matrix, Side side, PieceType... pieceTypes) {
        return getUnfilteredPiecesMoves(game, matrix, side, false, pieceTypes);
    }

    private Set<PointDTO> getUnfilteredPiecesMoves(Game game, CellsMatrix matrix, Side side, boolean isDefensive, PieceType... pieceTypes) {
        return matrix
                .somePiecesStream(side, pieceTypes)
                .map(moveableCell -> getUnfilteredAvailableMoves(game, matrix, moveableCell, isDefensive))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    /**
     * 1) Проверяет не встали ли мы под шах от простой фигуры (коня/слона/ладьи/ферзя).
     * 2) Так же проверяет, не передвинули ли мы фигуру, защищающую короля от шаха вражеской простой фигуры
     * TODO: can optimize:
     * если понадобится лучшая производительность: пункт №2 -> эту проверку нужно будет делать ДО вычисления доступных ходов.
     * ведь если фигурой ходить нельзья, зачем для нее находить доступные ходы вообще, когда можно вернуть пустой set?
     */
    private Predicate<PointDTO> isKingNotAttackedByEnemy(CellDTO moveableCell) {
        //TODO: can optimize
        PointDTO allyKingPoint = originalMatrix.findKingPoint(moveableCell.getSide());

        return pointTo -> {
            //имитируем ход
            MoveDTO move = MoveDTO.valueOf(moveableCell.getPoint(), pointTo, null);
            MoveResult moveResult = originalMatrix.executeMove(move, null);
            CellsMatrix newMatrix = moveResult.getNewMatrix();

            //для всех дальнобойных фигур собираем все доступные ходы врага на следующий ход
            Set<PointDTO> unfilteredEnemyMoves = getUnfilteredPiecesMoves(game, newMatrix, moveableCell.getEnemySide(),
                    PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN);

            //и проверяем, что мы не поставили нашего короля под атаку
            boolean isCanAttack;
            if (moveableCell.getPieceType() == PieceType.KING) {
                //если мы пошли королем, то не встал ли он под шах
                isCanAttack = unfilteredEnemyMoves.contains(pointTo);
            } else {
                //если другой фигурой - а не открылся ли шах
                isCanAttack = unfilteredEnemyMoves.contains(allyKingPoint);
            }

            return !isCanAttack;
        };
    }

    /**
     * Проверяет не встали ли мы королем под шах пешки противника
     */
    private Predicate<? super PointDTO> isKingNotAttackedByEnemyPawns(CellDTO moveableCell) {
        Side enemySide = moveableCell.getEnemySide();
        //TODO: can optimize
        PointDTO allyKingPoint = originalMatrix.findKingPoint(moveableCell.getSide());

        if (moveableCell.getPieceType() == PieceType.KING) {
            //если ходим королем, то не даем ему пойти под шах от пешки
            Set<PointDTO> enemyPawnAttackMoves = findPawnAttackMoves(enemySide, null);
            return pointTo -> !enemyPawnAttackMoves.contains(pointTo);

        } else {
            if (game.getUnderCheckSide() == moveableCell.getSide()) {
                /*
                    Если другой фигурой (не королем), но при этом наш король находится под шахом - значит что ему шахует вражеская пешка:
                    1) пешка - потому, что в предыдущей проверке = isKingNotAttackedByEnemy мы проверили все фигуры кроме пешки (KNIGHT, BISHOP, ROOK, QUEEN).
                    2) проверка - не встали ли мы под шах от вражеского короля происходит в методе = isNotAttackingByEnemyKing, вызываемом в методе getMovesForKing.

                    то... не даем фигурам ходить, кроме случая если она рубит пешку объявившую шах
                 */
                //TODO: can optimize
                return pointTo -> !findPawnAttackMoves(enemySide, pointTo).contains(allyKingPoint);
            } else {
                /*
                    если король не под шахом то шаха от вражеской пешки быть не может
                    ведь шаха не было, а ходим мы не королем
                 */
                return pointTo -> true;
            }
        }
    }

    private Set<PointDTO> findPawnAttackMoves(Side side, PointDTO excludePoint) {
        Set<PointDTO> enemyPawnPoints = originalMatrix.findPiecesCoords(side, PieceType.PAWN);
        enemyPawnPoints.remove(excludePoint);

        int vector = side.getPawnMoveVector();

        Set<PointDTO> pawnAttackMoves = new HashSet<>();
        enemyPawnPoints.forEach(point -> {
            pawnAttackMoves.add(PointDTO.valueOf(point.getRowIndex() + vector, point.getColumnIndex() + 1));
            pawnAttackMoves.add(PointDTO.valueOf(point.getRowIndex() + vector, point.getColumnIndex() - 1));
        });

        return pawnAttackMoves;
    }


    /**
     * Проверка на рокировку через битое поле
     */
    private Set<PointDTO> getUnavailableCastlingPoints(PointDTO pointFrom, Set<PointDTO> moves) {
        return moves.stream()
                .filter(pointFrom::isNotBorderedBy)                                                                     //если pointTo не граничит с позицией короля значит это рокировка
                .filter(castlingPoint -> {
                    int castlingVector = (castlingPoint.getColumnIndex() - pointFrom.getColumnIndex()) / 2;             //определяем вектор рокировки
                    int crossColumnIndex = pointFrom.getColumnIndex() + castlingVector;                                 //прибавляем к позиции короля
                    PointDTO crossPoint = PointDTO.valueOf(pointFrom.getRowIndex(), crossColumnIndex);                  //получаем пересекаемое при рокировке поле

                    //проверяем есть ли оно среди доступных ходов (если нет -> значит оно битое -> значит рокировка в эту сторону запрещена)
                    return !moves.contains(crossPoint);
                }).collect(Collectors.toSet());
    }


    private Set<PointDTO> getUnfilteredAvailableMoves(Game game, CellsMatrix matrix, CellDTO moveableCell, boolean isDefensive) {
        moveableCell.requireNotEmpty();
        InternalMoveHelper helper = new InternalMoveHelper(game, matrix, moveableCell, isDefensive);
        Set<PointDTO> moves;

        //noinspection ConstantConditions
        switch (moveableCell.getPieceType()) {
            case PAWN: {
                moves = helper.getMovesForPawn();
                break;
            }
            case KNIGHT: {
                moves = helper.getMovesForKnight();
                break;
            }
            case BISHOP: {
                moves = helper.getMovesForBishop();
                break;
            }
            case ROOK: {
                moves = helper.getMovesForRook();
                break;
            }
            case QUEEN: {
                moves = helper.getMovesForQueen();
                break;
            }
            case KING: {
                moves = helper.getMovesForKing();
                break;
            }
            default:
                moves = new HashSet<>();
        }
        return moves;
    }

    @SuppressWarnings({"ConstantConditions", "PointlessArithmeticExpression"})
    private class InternalMoveHelper {

        private final Game game;
        private final CellsMatrix matrix;
        private final CellDTO moveableCell;
        private final boolean isDefensive;

        private InternalMoveHelper(Game game, CellsMatrix matrix, CellDTO moveableCell, boolean isDefensive) {
            this.game = game;
            this.matrix = matrix;
            this.moveableCell = moveableCell;
            this.isDefensive = isDefensive;

            moveableCell.requireNotEmpty();
        }

        private Set<PointDTO> getMovesForPawn() {
            Set<PointDTO> moves = new HashSet<>();

            int activeRow = moveableCell.getRowIndex();
            int activeColumn = moveableCell.getColumnIndex();
            Side allySide = moveableCell.getSide();

            int vector = allySide.getPawnMoveVector();

            boolean isFirstMove = false;
            if (activeRow == 1 || activeRow == 6) {
                isFirstMove = true;
            }

            CellDTO cell;

            if (!isDefensive) {
                cell = getCell(activeRow + 1 * vector, activeColumn);
                boolean isAdded = addPawnMove(moves, cell, allySide, false);

                if (isFirstMove && isAdded) {
                    cell = getCell(activeRow + 2 * vector, activeColumn);
                    addPawnMove(moves, cell, allySide, false);
                }
            }

            //attack
            cell = getCell(activeRow + vector, activeColumn + 1);
            addPawnMove(moves, cell, allySide, true);

            cell = getCell(activeRow + vector, activeColumn - 1);
            addPawnMove(moves, cell, allySide, true);

            if (!isDefensive) {
                Integer enemyLongMoveColumnIndex = game.getPawnLongMoveColumnIndex(moveableCell.getEnemySide());
                if (enemyLongMoveColumnIndex != null) {    //противник только что сделал длинный ход пешкой

                    if (Math.abs(enemyLongMoveColumnIndex - activeColumn) == 1) {    //и эта пешка рядом с выделенной (слева или справа)

                        if (activeRow == 3 || activeRow == 4) {        //и это творится на нужной горизонтали
                            //значит можно делать взятие на проходе
                            //проверять ничего не нужно, эта ячейка 100% пуста (не могла же пешка перепрыгнуть фигуру)
                            //noinspection ConstantConditions
                            moves.add(PointDTO.valueOf(activeRow + allySide.getPawnMoveVector(), enemyLongMoveColumnIndex));
                        }
                    }
                }
            }

            return moves;
        }

        private Set<PointDTO> getMovesForKnight() {
            Set<PointDTO> moves = new HashSet<>();

            addKnightMove(moves, 1, 2);
            addKnightMove(moves, 2, 1);
            addKnightMove(moves, 1, -2);
            addKnightMove(moves, 2, -1);
            addKnightMove(moves, -1, 2);
            addKnightMove(moves, -2, 1);
            addKnightMove(moves, -1, -2);
            addKnightMove(moves, -2, -1);

            return moves;
        }

        private Set<PointDTO> getMovesForBishop() {
            Set<PointDTO> moves = new HashSet<>();

            addAvailableMovesForRay(moves, 1, 1);
            addAvailableMovesForRay(moves, -1, 1);
            addAvailableMovesForRay(moves, 1, -1);
            addAvailableMovesForRay(moves, -1, -1);

            return moves;
        }

        private Set<PointDTO> getMovesForRook() {
            Set<PointDTO> moves = new HashSet<>();

            addAvailableMovesForRay(moves, 1, 0);
            addAvailableMovesForRay(moves, -1, 0);
            addAvailableMovesForRay(moves, 0, 1);
            addAvailableMovesForRay(moves, 0, -1);

            return moves;
        }

        private Set<PointDTO> getMovesForQueen() {
            Set<PointDTO> moves = new HashSet<>();

            moves.addAll(getMovesForRook());
            moves.addAll(getMovesForBishop());

            return moves;
        }

        private Set<PointDTO> getMovesForKing() {
            Set<PointDTO> moves = new HashSet<>();

            addAvailableMovesForRay(moves, 1, 0, 1);
            addAvailableMovesForRay(moves, -1, 0, 1);
            addAvailableMovesForRay(moves, 0, 1, 1);
            addAvailableMovesForRay(moves, 0, -1, 1);
            addAvailableMovesForRay(moves, 1, 1, 1);
            addAvailableMovesForRay(moves, -1, 1, 1);
            addAvailableMovesForRay(moves, 1, -1, 1);
            addAvailableMovesForRay(moves, -1, -1, 1);

            if (isDefensive) {
                return moves;
            }

            if (game.isShortCastlingAvailable(moveableCell.getSide())) {
                if (isEmptyCellsByActiveRow(1, 2)) {
                    addMove(moves, getCell(moveableCell.getRowIndex(), moveableCell.getColumnIndex() - 2), moveableCell.getSide());
                }
            }
            if (game.isLongCastlingAvailable(moveableCell.getSide())) {
                if (isEmptyCellsByActiveRow(4, 5, 6)) {
                    addMove(moves, getCell(moveableCell.getRowIndex(), moveableCell.getColumnIndex() + 2), moveableCell.getSide());
                }
            }

            return moves.stream()
                    //фильровать здесь!!!
                    //если это делать там, где происходят остальные проверки на шах - попадем в бесконечную рекурсию
                    .filter(isNotAttackedByEnemyKing(moveableCell.getEnemySide()))
                    .collect(Collectors.toSet());
        }

        private void addAvailableMovesForRay(Set<PointDTO> moves, int rowVector, int columnVector) {
            addAvailableMovesForRay(moves, rowVector, columnVector, 7);
        }

        private void addAvailableMovesForRay(Set<PointDTO> moves, int rowVector, int columnVector, int rayLength) {
            for (int i = 1; i < rayLength + 1; i++) {
                CellDTO cell = getCell(moveableCell.getRowIndex() + rowVector * i, moveableCell.getColumnIndex() + columnVector * i);
                if (!addMove(moves, cell, moveableCell.getSide())) {
                    break;
                }
            }
        }

        /**
         * Добавляет point(от cell) в moves, в случае если данный ход доступен в рамках правил игры:
         * - происходит перемещение фигуры на пустую ячейку
         * if (isDefensive == false)
         * - происходит перемещение фигуры на ячейку с фигурой противника (взятие)
         * else (isDefensive = true)
         * - происходит перемещение фигуры на ячейку с союзной фигурой (защита фигуры)
         * <p>
         * Возвращаемое значение нужно для поиска доступных ходов по вектору (для фигур BISHOP, ROOK, QUEEN).
         * <p>
         * return false - если мы вышли за край доски или же ячейка не пуста, иначе - true
         */
        private boolean addMove(Set<PointDTO> moves, CellDTO cell, Side allySide) {
            Side skipSide = isDefensive ? allySide.reverse() : allySide;

            if (cell == null || cell.getSide() == skipSide) {
                return false;
            }

            if (!isDefensive || cell.getSide() == allySide) {
                moves.add(cell.getPoint());
            }

            return cell.isEmpty();
        }

        private void addKnightMove(Set<PointDTO> moves, int rowOffset, int columnOffset) {
            Side expectedSide = isDefensive ? moveableCell.getSide() : moveableCell.getEnemySide();

            CellDTO cell = getCell(moveableCell.getRowIndex() + rowOffset, moveableCell.getColumnIndex() + columnOffset);

            if (cell != null && (cell.getSide() == expectedSide || (!isDefensive && cell.isEmpty()))) {
                moves.add(cell.getPoint());
            }
        }

        private boolean addPawnMove(Set<PointDTO> moves, CellDTO cell, Side allySide, boolean isAttack) {
            Side unexpectedSide = isDefensive ? allySide.reverse() : allySide;
            if (cell == null || cell.getSide() == unexpectedSide) {
                return false;
            }

            if (isAttack) {
                if (cell.getSide() == unexpectedSide.reverse()) {
                    moves.add(cell.getPoint());
                }
                return false;
            } else {
                if (isDefensive) {
                    throw new UnsupportedOperationException("You can't call this method when (isDefensive == true && isAttack == false)");
                }

                if (cell.getSide() == allySide.reverse()) {
                    return false;
                } else {
                    moves.add(cell.getPoint());
                    return true;
                }
            }
        }

        /**
         * Проверяет не встали ли мы королем под шах вражеского короля.
         */
        private Predicate<PointDTO> isNotAttackedByEnemyKing(Side enemySide) {
            PointDTO enemyKingPoint = findKingPoint(enemySide);
            return enemyKingPoint::isNotBorderedBy;  //или может подошли вплотную к вражескому королю?
        }

        private boolean isEmptyCellsByActiveRow(int... columnIndexes) {
            for (int columnIndex : columnIndexes) {
                if (!isEmptyCell(moveableCell.getRowIndex(), columnIndex)) {
                    return false;
                }
            }

            return true;
        }

        private CellDTO getCell(int rowIndex, int columnIndex) {
            try {
                return matrix.getCell(rowIndex, columnIndex);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }

        private boolean isEmptyCell(int rowIndex, int columnIndex) {
            return matrix.isEmptyCell(rowIndex, columnIndex);
        }

        private PointDTO findKingPoint(Side side) {
            return matrix.findKingPoint(side);
        }
    }


    @Override
    public Stream<ExtendedMove> getExtendedMovesStream(Side side) {
        return getExtendedCellStream(side)
                .flatMap(toExtendedMovesStream(originalMatrix));
    }

    private Stream<ExtendedCell> getExtendedCellStream(Side side) {
        return originalMatrix
                .allPiecesBySideStream(side)
                .map(cellFrom -> {
                    Set<PointDTO> availableMoves = getFilteredAvailableMoves(cellFrom);
                    return new ExtendedCell(cellFrom, availableMoves);
                });
    }

    private Function<ExtendedCell, Stream<? extends ExtendedMove>> toExtendedMovesStream(CellsMatrix matrix) {
        return moves -> moves.getAvailablePoints().stream().map(pointTo -> new ExtendedMove(moves.getCellFrom(), matrix.getCell(pointTo)));
    }

}