package com.example.chess.logic.objects.game;

import com.example.chess.dto.PointDTO;
import com.example.chess.enums.Piece;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.CheckmateException;
import com.example.chess.logic.MoveHelper;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.ExtendedMove;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.example.chess.logic.utils.CommonUtils.tabs;

@Log4j2
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class GameContext {

    final RootGameContext root;
    final GameContext parent;
    final FakeGame game;        //game state after lastMove

    final CellsMatrix matrix;   //matrix state after lastMove
    final ExtendedMove lastMove;

    //key = pointTo
    Map<PointDTO, List<GameContext>> children;  //возможно стоит поменять на array[][][]

    @Setter boolean isCheckmate;
    @Setter boolean isDeepExchangeAlreadyCalculated = false;

    public void fill(int maxDeep) {
        fill(maxDeep, move -> true);
    }

    public void fill(int deep, Predicate<ExtendedMove> movesFilter) {
        MoveHelper.valueOf(this)
                .getStandardMovesStream(nextTurnSide())
                .filter(movesFilter)
                .sorted(Comparator.comparing(ExtendedMove::getValueFrom))
                .map(this::executeMove)
                .filter(childContext -> deep > 1)
                .forEach(childContext -> childContext.fill(deep - 1));
    }

    private GameContext executeMove(ExtendedMove nextMove) {
        Piece pieceFrom = matrix.getCell(nextMove.getPointFrom()).getPiece();

        FakeGame nextGame = game.executeMove(nextMove, pieceFrom);
        CellsMatrix nextMatrix = matrix.executeMove(nextMove);

        RootGameContext rootContext = isRoot() ? (RootGameContext) this : root;
        GameContext childContext = new GameContext(rootContext, this, nextGame, nextMatrix, nextMove);

        addChild(childContext);
        return childContext;
    }

    private void addChild(GameContext childNode) {
        if (children == null) {
            children = new HashMap<>();
        }
        ExtendedMove childMove = childNode.getLastMove();

        List<GameContext> internalList = children.computeIfAbsent(childMove.getPointTo(), key -> new ArrayList<>());
        internalList.add(childNode);
    }

    public int getPosition() {
        return matrix.getPosition();
    }

    public Side nextTurnSide() {
        return lastMoveSide().reverse();
    }

    public Side lastMoveSide() {
        if (lastMove == null) {
            //if bot has first move in game
            return Side.BLACK;
        }
        return lastMove.getSide();
    }

    public boolean botNext() {
        return root == null || root.getBotSide() == nextTurnSide();
    }

    public boolean botLast() {
        return root == null || root.getBotSide() == lastMoveSide();
    }

    public boolean isRoot() {
        return root == null && parent == null;
    }

    public long getTotalMovesCount() {
        if (children == null) {
            return 1;
        }

        return childrenStream()
                .mapToLong(GameContext::getTotalMovesCount)
                .sum() + 1;
    }

    public Stream<GameContext> childrenStream() {
        return children.values()
                .stream()
                .flatMap(Collection::stream);
    }

    public Stream<GameContext> childrenStream(PointDTO targetPoint) {
        List<GameContext> internalList = children.get(targetPoint);
        if (internalList == null) {
            return Stream.empty();
        }
        return internalList.stream();
    }

    public Stream<GameContext> neighboursStream() {
        if (parent != null) {
            return parent.childrenStream()
                    .filter(context -> context != this);
        }
        return Stream.empty();
    }

    public int getDeep() {
        int deep = 0;
        GameContext parent = this.getParent();
        while (parent != null) {
            deep++;
            parent = parent.getParent();
        }

        return deep;
    }

    public boolean hasChildren() {
        return children != null;
    }

    public int getTotal() {
        int total = lastMove.getTotal();
        GameContext maxDeeperContext = findMaxChildren();

        while (maxDeeperContext != null) {
            if (maxDeeperContext.botLast()) {
                total += maxDeeperContext.getLastMove().getTotal();
            } else {
                total -= maxDeeperContext.getLastMove().getTotal();
            }

            maxDeeperContext = maxDeeperContext.findMaxChildren();
        }

        return total;
        //TODO: check by checkmate   -> context.isCheckmate() -> isCheckmateByBot/Player
        //TODO: deeper moves ratio? (without checkmate)
    }

    private GameContext findMaxChildren() {
        if (hasChildren()) {
            return childrenStream().reduce((BinaryOperator.maxBy(Comparator.comparing(GameContext::getTotal)))).orElseThrow(() -> new CheckmateException(this));
        }
        return null;
    }

    public void print(int tabsCount, String prefix) {
        printMove(tabsCount, prefix);

        if (tabsCount == 0) {
            getLastMove().printRating(tabsCount + 1);
        }

        System.out.println("--------------------------------");
        printMinMax(tabsCount + 1);
    }

    private void printMinMax(int tabsCount) {
        if (hasChildren()) {
            childrenStream()
                    .reduce((BinaryOperator.maxBy(Comparator.comparing(GameContext::getTotal))))
                    .ifPresent(maxContext -> {
                        maxContext.printMove(tabsCount, maxContext.getPrefix("max"), maxContext.getContextPostfix());
                        maxContext.printMinMax(tabsCount + 1);
                    });

            childrenStream()
                    .reduce((BinaryOperator.minBy(Comparator.comparing(GameContext::getTotal))))
                    .ifPresent(minContext -> {
                        minContext.printMove(tabsCount, minContext.getPrefix("min"), minContext.getContextPostfix());
                        minContext.printMinMax(tabsCount + 1);
                    });
        }
    }

    public void printMove(int tabsCount, String prefix) {
        printMove(tabsCount, prefix, "");
    }

    public void printMove(int tabsCount, String prefix, String postfix) {
        System.out.println(tabs(tabsCount) + prefix + "[" + getLastMove() + "].total = " + getTotal() + postfix);
    }

    private String getPrefix(String prefix) {
        if (botLast()) {
            return prefix + "BotMove";
        }
        return prefix + "PlayerMove";
    }

    private String getContextPostfix() {
        if (!hasChildren()) {
            return " [-]";
        }
        return "";
    }
}
