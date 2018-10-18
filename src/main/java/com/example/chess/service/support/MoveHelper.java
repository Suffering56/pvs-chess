package com.example.chess.service.support;

import com.example.chess.Debug;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.KingNotFoundException;
import com.example.chess.exceptions.UnattainablePointException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"ConstantConditions", "Duplicates"})
public class MoveHelper {

    private final FakeGame fakeGame;
    private final CellsMatrix originalMatrix;

    private MoveHelper(FakeGame fakeGame, CellsMatrix originalMatrix) {
        this.fakeGame = fakeGame;
        this.originalMatrix = originalMatrix;
    }

    public static MoveHelper valueOf(FakeGame fakeGame, CellsMatrix originalMatrix) {
        Debug.incrementMoveHelpersCount();
        return new MoveHelper(fakeGame, originalMatrix);
    }

    public boolean isKingUnderAttack(Side kingSide) {
        PointDTO kingPoint = originalMatrix.getKingPoint(kingSide);
        Side enemySide = kingSide.reverse();

        Set<PointDTO> enemyMoves = originalMatrix
                .excludePiecesStream(enemySide, PieceType.KING)
                .map(enemyCell -> getUnfilteredMovesForCell(enemyCell, false))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        return enemyMoves.contains(kingPoint);
    }

    public Set<PointDTO> getFilteredAvailablePoints(PointDTO pointFrom) {
        CellDTO moveableCell = originalMatrix.getCell(pointFrom);
        FilterData filterData = createFilterData(moveableCell.getSide());

        return getFilteredMovesForCell(moveableCell, filterData, false);
    }

    public Stream<ExtendedMove> getStandardMovesStream(Side side) throws KingNotFoundException {
        FilterData filterData = createFilterData(side);

        return originalMatrix
                .allPiecesBySideStream(side)
                .flatMap(moveableCell -> {
                    Set<PointDTO> filteredMoves = getFilteredMovesForCell(moveableCell, filterData, false);
                    return filteredMoves.stream().map(pointTo -> new ExtendedMove(moveableCell, originalMatrix.getCell(pointTo)));
                });
    }

    private FilterData createFilterData(Side side) {
        Side enemySide = side.reverse();
        PointDTO kingPoint = originalMatrix.getKingPoint(side);
        Map<PointDTO, UnmovableData> unmovablePointsMap = getUnmovablePointsMap(enemySide, kingPoint);

        Set<PointDTO> enemyDefensivePoints = originalMatrix
                .allPiecesBySideStream(enemySide)
                .flatMap(enemyCell -> getUnfilteredMovesForCell(enemyCell, true).stream())
                .collect(Collectors.toSet());


        FilterData filterData = new FilterData(enemyDefensivePoints, unmovablePointsMap);
        initFilterData(kingPoint, enemySide, filterData);
        updateFilterData(filterData, kingPoint);
        return filterData;
    }

    private void initFilterData(PointDTO kingPoint, Side enemySide, FilterData filterData) {

        originalMatrix
                .allPiecesBySideStream(enemySide)
                .forEach(enemyCell -> {
                    Set<PointDTO> enemyMoves;
                    if (enemyCell.getPieceType() == PieceType.PAWN) {
                        enemyMoves = getPawnDiagonalMoves(enemyCell, enemySide);
                    } else {
                        enemyMoves = getUnfilteredMovesForCell(enemyCell, false);
                    }

                    filterData.enemyPoints.addAll(enemyMoves);

                    if (enemyMoves.contains(kingPoint)) {
                        //нам УЖЕ шах! => мы далеко не каждой фигурой можем ходить
                        if (filterData.sourceOfCheck == null) {
                            filterData.sourceOfCheck = enemyCell;
                        } else {
                            filterData.isMultiCheck = true;
                        }
                    }
                });
    }

    private void updateFilterData(FilterData filterData, PointDTO kingPoint) {
        CellDTO source = filterData.sourceOfCheck;

        if (source != null && !filterData.isMultiCheck) {
            switch (source.getPieceType()) {

                case PAWN:
                case KNIGHT:
                    filterData.shieldPoints = null;
                    filterData.excludePoints = null;
                    break;
                case BISHOP:
                case ROOK:
                case QUEEN:
                    filterData.shieldPoints = calculateShieldPoints(source, kingPoint);
                    filterData.excludePoints = calculateExcludePoints(source, kingPoint);
                    break;
                case KING:
                    throw new UnattainablePointException();
            }
        }
    }

    private Set<PointDTO> calculateExcludePoints(CellDTO source, PointDTO kingPoint) {
        Set<PointDTO> excludePoints = new HashSet<>();
        BetweenParams params = createBetweenParams(kingPoint, source);
        int[] directionArray = {-1, 1};

        for (int direction : directionArray) {
            int rowOffset = direction * params.rowVector;
            int columnOffset = direction * params.columnVector;

            int rowIndex = kingPoint.getRowIndex() + rowOffset;
            int columnIndex = kingPoint.getColumnIndex() + columnOffset;

            if (PointDTO.isCorrectIndex(rowIndex, columnIndex)) {
                PointDTO excludePoint = PointDTO.valueOf(rowIndex, columnIndex);

                if (!source.getPoint().equals(excludePoint)) {
                    excludePoints.add(excludePoint);
                }
            }
        }

        return excludePoints;
    }

    private Set<PointDTO> calculateShieldPoints(CellDTO source, PointDTO kingPoint) {
        BetweenParams betweenParams = createBetweenParams(kingPoint, source);
        Objects.requireNonNull(betweenParams);
        return findBetweenPoints(kingPoint, betweenParams, null);
    }

    private Set<PointDTO> findBetweenPoints(PointDTO kingPoint, BetweenParams params, PointDTO excludePoint) {
        Set<PointDTO> points = new HashSet<>();

        //увеличиваем params.rayLength на один, т.к. в данном случае можем рубить источник шаха
        for (int i = 1; i <= params.rayLength + 1; i++) {
            int rowOffset = i * params.rowVector;
            int columnOffset = i * params.columnVector;

            PointDTO point = PointDTO.valueOf(kingPoint.getRowIndex() + rowOffset,
                    kingPoint.getColumnIndex() + columnOffset);

            if (!point.equals(excludePoint)) {
                points.add(point);
            }
        }

        return points;
    }

    private Map<PointDTO, UnmovableData> getUnmovablePointsMap(Side enemySide, PointDTO kingPoint) {
        return originalMatrix
                .includePiecesStream(enemySide, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN)
                .map(enemyPossibleAttackerCell -> {
                    BetweenParams betweenParams = createBetweenParams(kingPoint, enemyPossibleAttackerCell);

                    CellDTO unmovableCell = null;
                    if (betweenParams != null) {
                        unmovableCell = findUnmovableCell(kingPoint, betweenParams);
                    }

                    if (unmovableCell == null) {
                        return null;
                    }

                    Set<PointDTO> availablePoints = null;
                    PieceType unmovablePieceType = unmovableCell.getPieceType();

                    if (unmovablePieceType == PieceType.QUEEN
                            || (betweenParams.isDiagonal && unmovablePieceType == PieceType.BISHOP)
                            || (!betweenParams.isDiagonal && unmovablePieceType == PieceType.ROOK)) {

                        availablePoints = findBetweenPoints(kingPoint, betweenParams, unmovableCell.getPoint());

                    } else if (unmovablePieceType == PieceType.PAWN) {
                        availablePoints = findAvailablePointsForPawn(kingPoint, betweenParams, unmovableCell, enemyPossibleAttackerCell);
                    }

                    return new UnmovableData(unmovableCell.getPoint(), availablePoints);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UnmovableData::getUnmovableCellPoint, Function.identity()));
    }

    private Set<PointDTO> findAvailablePointsForPawn(PointDTO kingPoint, BetweenParams betweenParams, CellDTO unmovablePawn, CellDTO enemyCell) {
        Set<PointDTO> pawnUnfilteredMoves = getUnfilteredMovesForCell(unmovablePawn, false);

        if (pawnUnfilteredMoves.contains(enemyCell.getPoint())) {
            return new HashSet<PointDTO>() {{
                add(enemyCell.getPoint());
            }};
        }

        if (!betweenParams.isDiagonal) {
            findBetweenPoints(kingPoint, betweenParams, unmovablePawn.getPoint());

            Set<PointDTO> result = new HashSet<>();
            if (unmovablePawn.getColumnIndex().equals(enemyCell.getColumnIndex())) {
                for (PointDTO pawnMove : pawnUnfilteredMoves) {
                    if (unmovablePawn.getColumnIndex().equals(pawnMove.getColumnIndex())) {
                        result.add(pawnMove);
                    }
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        return null;
    }

    private BetweenParams createBetweenParams(PointDTO kingPoint, CellDTO enemyPossibleAttackerCell) {
        switch (enemyPossibleAttackerCell.getPieceType()) {
            case BISHOP:
                return createBetweenParamsForBishop(kingPoint, enemyPossibleAttackerCell);
            case ROOK:
                return createBetweenParamsForRook(kingPoint, enemyPossibleAttackerCell);
            case QUEEN:
                BetweenParams betweenParams = createBetweenParamsForBishop(kingPoint, enemyPossibleAttackerCell);
                if (betweenParams == null) {
                    return createBetweenParamsForRook(kingPoint, enemyPossibleAttackerCell);
                }
                return betweenParams;
            default:
                throw new UnattainablePointException();
        }
    }

    private BetweenParams createBetweenParamsForRook(PointDTO kingPoint, CellDTO cell) {
        int diff;
        int rowVector = 0;
        int columnVector = 0;

        if (kingPoint.getRowIndex() == cell.getRowIndex()) {
            diff = kingPoint.getColumnIndex() - cell.getColumnIndex();
            columnVector = diff > 0 ? -1 : 1;

        } else if (kingPoint.getColumnIndex() == cell.getColumnIndex()) {
            diff = kingPoint.getRowIndex() - cell.getRowIndex();
            rowVector = diff > 0 ? -1 : 1;
        } else {
            return null;
        }

        int rayLength = Math.abs(diff) - 1;
        return new BetweenParams(rayLength, rowVector, columnVector, false);
    }

    private BetweenParams createBetweenParamsForBishop(PointDTO kingPoint, CellDTO cell) {
        int rowDiff = kingPoint.getRowIndex() - cell.getRowIndex();
        int columnDiff = kingPoint.getColumnIndex() - cell.getColumnIndex();

        if (Math.abs(rowDiff) == Math.abs(columnDiff)) {
            int rayLength = Math.abs(rowDiff) - 1;

            int rowVector = rowDiff > 0 ? -1 : 1;
            int columnVector = columnDiff > 0 ? -1 : 1;
            return new BetweenParams(rayLength, rowVector, columnVector, true);
        }

        return null;
    }

    private CellDTO findUnmovableCell(PointDTO kingPoint, BetweenParams params) {
        int piecesBetweenCount = 0;
        CellDTO unmovableCell = null;

        for (int i = 1; i <= params.rayLength; i++) {
            int rowOffset = i * params.rowVector;
            int columnOffset = i * params.columnVector;

            CellDTO betweenCell = getCellByKingPoint(kingPoint, rowOffset, columnOffset);
            if (!betweenCell.isEmpty()) {
                piecesBetweenCount++;
                if (unmovableCell == null) {
                    unmovableCell = betweenCell;
                }
            }
        }

        if (piecesBetweenCount == 1) {
            return unmovableCell;
        }
        return null;
    }

    private Set<PointDTO> getPawnDiagonalMoves(CellDTO movableCell, Side enemySide) {
        Set<PointDTO> moves = new HashSet<>();

        PointDTO point = movableCell.getPoint();
        int vector = enemySide.getPawnMoveVector();
        int rowIndex = point.getRowIndex() + vector;

        if (PointDTO.isCorrectIndex(rowIndex, point.getColumnIndex() + 1)) {
            moves.add(PointDTO.valueOf(rowIndex, point.getColumnIndex() + 1));
        }
        if (PointDTO.isCorrectIndex(rowIndex, point.getColumnIndex() - 1)) {
            moves.add(PointDTO.valueOf(rowIndex, point.getColumnIndex() - 1));
        }

        return moves;
    }

    private Set<PointDTO> getUnfilteredMovesForCell(CellDTO moveableCell, boolean isDefensive) {
        return getMovesForCell(moveableCell, null, isDefensive);
    }

    private Set<PointDTO> getFilteredMovesForCell(CellDTO moveableCell, FilterData filterData, boolean isDefensive) {
        return getMovesForCell(moveableCell, Objects.requireNonNull(filterData), isDefensive);
    }

    /**
     * Не вызывай этот метод ниоткуда, кроме addUnfilteredMovesForCell()/addFilteredMovesForCell(), ладно?
     */
    private Set<PointDTO> getMovesForCell(CellDTO moveableCell, FilterData filterData, boolean isDefensive) {
        Set<PointDTO> moves = new InternalMoveHelper(moveableCell, filterData, isDefensive)
                .getAnyPieceMoves();

        Debug.incrementAddMovesForCallsCount();
        Debug.incrementAvailablePointsFound(moves.size());

        return moves;
    }

    @SuppressWarnings({"PointlessArithmeticExpression"})
    private class InternalMoveHelper {

        private final CellDTO movableCell;
        private final Set<PointDTO> moves;
        private final FilterData filterData;
        private final Side allySide;
        private final Side expectedSide;
        private final boolean isDefensive;

        private InternalMoveHelper(CellDTO movableCell, FilterData filterData, boolean isDefensive) {
            movableCell.requireNotEmpty();

            this.isDefensive = isDefensive;
            this.filterData = filterData;
            this.movableCell = movableCell;
            this.moves = new HashSet<>();
            this.allySide = movableCell.getSide();

            if (!isDefensive) { //normal
                expectedSide = allySide.reverse();
            } else {            //defensive
                expectedSide = allySide;
            }
        }

        private Set<PointDTO> getAnyPieceMoves() {
            PieceType movablePieceType = movableCell.getPieceType();

            if (isCheckFilterEnabled() && movablePieceType != PieceType.KING) {

                if (filterData.isMultiCheck) {
                    return moves;
                }

                UnmovableData unmovableData = filterData.unmovablePointsMap.get(movableCell.getPoint());
                if (unmovableData != null) {
                    Set<PointDTO> availablePoints = unmovableData.getAvailablePoints();
                    if (availablePoints != null) {
                        moves.addAll(availablePoints);
                    }
                    return moves;
                }
            }

            switch (movablePieceType) {
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


            if (isCheckFilterEnabled() && movablePieceType != PieceType.KING) {

                CellDTO sourceOfCheck = filterData.sourceOfCheck;
                if (sourceOfCheck != null) {                        //значит нам шах

                    PointDTO sourcePoint = sourceOfCheck.getPoint();
                    if (movablePieceType == PieceType.PAWN && movableCell.getColumnIndex() == sourcePoint.getColumnIndex()) {
                        return new HashSet<>();
                    }

                    if (filterData.shieldPoints != null) {          //шах от слона/ладьи/ферзя -> значит можно или срубить или закрыться
                        moves.retainAll(filterData.shieldPoints);   //в moves - теперь пересечение moves и shieldPoints
                        return moves;
                    }

                    if (moves.contains(sourcePoint)) {
                        return new HashSet<PointDTO>() {{
                            add(sourcePoint);
                        }};
                    }
                    return new HashSet<>();
                }
            }

            return moves;
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

            addPawnAttackMoves(vector);

            if (isDefensive) {
                return;
            }

            boolean isAdded = addPawnHarmlessMove(1 * vector);
            if (isAdded) {
                boolean isFirstMove = currentRow == 1 || currentRow == 6;
                if (isFirstMove) {
                    addPawnHarmlessMove(2 * vector);
                }
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
        private boolean addPawnHarmlessMove(int rowIndexOffset) {
            CellDTO cell = getCellByOffsets(rowIndexOffset, 0);
            if (cell != null && cell.isEmpty()) {
                moves.add(cell.getPoint());
                return true;
            }

            return false;
        }

        /**
         * @return true if move added, false - otherwise (addable cell is null or ally)
         */
        private boolean addMove(int rowIndexOffset, int columnIndexOffset) {
            CellDTO cell = getCellByOffsets(rowIndexOffset, columnIndexOffset);
            if (cell != null) {
                if (cell.isEmpty()) {
                    moves.add(cell.getPoint());
                    return true;
                }
                if (cell.getSide() == expectedSide) {
                    moves.add(cell.getPoint());
                    return false;
                }
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
            if (cell != null && (cell.isEmpty() || cell.getSide() == expectedSide)) {
                PointDTO point = cell.getPoint();

                if (!isCheckFilterEnabled()) {
                    moves.add(point);
                    return;
                }

                if (filterData.sourceOfCheck != null && filterData.excludePoints != null) {
                    //нам шах от слона, ладьи или ферзья (запрещаем королю оставаться на атакуемой вертикали/горизонтали/диагонали)
                    if (filterData.excludePoints.contains(point)) {
                        return;
                    }
                }

                if (cell.isEmpty()) {
                    if (!filterData.enemyPoints.contains(point)) {
                        moves.add(point);
                    }
                } else {
                    if (!filterData.enemyDefensivePoints.contains(point)) {
                        moves.add(point);
                    }
                }
            }
        }

        private boolean isSafeCrossPointForCastling(int columnIndexOffset) {
            PointDTO crossPoint = getPointByOffsets(0, columnIndexOffset);
            if (crossPoint != null) {
                return isCheckFilterEnabled() && !filterData.enemyPoints.contains(crossPoint);
            }
            return false;
        }

        private void addPawnAttackMoves(int vector) {
            CellDTO cell = getCellByOffsets(1 * vector, 1);
            if (cell != null && cell.getSide() == expectedSide) {
                moves.add(cell.getPoint());
            }

            cell = getCellByOffsets(1 * vector, -1);
            if (cell != null && cell.getSide() == expectedSide) {
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

        private boolean isCheckFilterEnabled() {
            return filterData != null;
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

    private class FilterData {
        private final Set<PointDTO> enemyPoints = new HashSet<>();
        private final Set<PointDTO> enemyDefensivePoints;
        private final Map<PointDTO, UnmovableData> unmovablePointsMap;

        private CellDTO sourceOfCheck = null;
        private boolean isMultiCheck = false;   //only king can move;
        private Set<PointDTO> shieldPoints;     //if sourceOfCheck == BISHOP, ROOK, QUEEN
        private Set<PointDTO> excludePoints;    //if sourceOfCheck == BISHOP, ROOK, QUEEN

        private FilterData(Set<PointDTO> enemyDefensivePoints, Map<PointDTO, UnmovableData> unmovablePointsMap) {
            this.enemyDefensivePoints = enemyDefensivePoints;
            this.unmovablePointsMap = unmovablePointsMap;
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private class UnmovableData {
        private PointDTO unmovableCellPoint;
        private Set<PointDTO> availablePoints;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private class BetweenParams {
        private int rayLength;
        private int rowVector;
        private int columnVector;
        private boolean isDiagonal;
    }
}
