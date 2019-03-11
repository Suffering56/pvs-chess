package com.example.chess.logic.objects.game;

import com.example.chess.App;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.Piece;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.CheckmateException;
import com.example.chess.logic.MoveHelper;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.Rating;
import com.example.chess.logic.objects.move.ExtendedMove;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.example.chess.logic.utils.CommonUtils.tabs;

@Log4j2
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * Основные концепции:
 * - рейтинг должен учитывать lastMoveSide но плюсовые значения - это те ходы которые выгодны боту
 *      а значит если playerMove.total == 300, то по сути для игрока это -300 - значит он где то фидит фигуры.
 *      PS: после переработки MaterialRatingCalculator в этой логике вообще не должно быть отрицательных значений
 *      так как невозможно самому срубить свою фигуру, а думаем мы на глубину 1
 * - ЛИБО при подсчетах рейтинга вообще не должен учитываться side:
 *      то есть если playerMove = +300, то это -300 для бота.
 *      так интуитивно понятнее и логичнее и не нужны все эти inverted флаги и коэффициенты
 *      А еще в будущем подобный подход будет легко использовать при анализе ходов игрока, а вот с предыдущим подходом
 *      реализация такой хотелки принесет много страданий
 *
 * В каких то местах работает одно правило, в каких-то другое. разберись с этим
 * Решено:
 * - используем правило #2:
 * а) стараемя полностью уйти от использования botLast/playerLast
 * b) переписываем deepExchange:
 *      - добавляем в контекст флаг: boolean exchangeStopped (включительно или нет надо думать)
 *      - метод isExchangeStopped должен всегда возвращать false если getDeep < (или <=) MAX
 * c) ну и соответственно поменяется реализация getContextTotal
 * (т.к. для игрока будет искаться теперь max а не min, но в то же время там теперь не всегда будет +=) не забудь учесть это и для тех у кого нет детей
 * d) INVERTED_MATERIAL_RATING - вот тут пока совсем непонятно, но о нем нельзя забывать, как и о том что там используется отрицательный коэффициент в RatingParam
 */
public class GameContext {

    final RootGameContext root;
    final GameContext parent;
    final FakeGame game;        //game state after lastMove

    final CellsMatrix matrix;   //matrix state after lastMove
    final ExtendedMove lastMove;

    //key = pointTo
    Map<PointDTO, List<GameContext>> children;  //возможно стоит поменять на array[][][]

    @Setter boolean isCheckmate;

    public void fill(int maxDeep) {
        fill(maxDeep, move -> true);
    }

    public void fillForExchange(PointDTO pointTo) {
        fill(Integer.MAX_VALUE, childMove -> childMove.hasSamePointTo(pointTo));
    }

    private void fill(int deep, Predicate<ExtendedMove> movesFilter) {
        MoveHelper.valueOf(this)
                .getStandardMovesStream(nextTurnSide())
                .filter(movesFilter)
                .sorted(Comparator.comparing(ExtendedMove::getValueFrom))
                .map(this::executeMove)
                .filter(childContext -> deep > 1)
                .forEach(childContext -> childContext.fill(deep - 1, movesFilter));
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

    private boolean playerLast() {
        return !botLast();
    }

    private boolean botLast() {
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

    private int getMoveTotal() {
        return lastMove.getTotal();
    }


    /**
     * В общем не знаю почему я так долго шел именно к вот такой реализации подсчета тотала,
     * но мне кажется она единственно верной.
     * На данный момент работает криво из-за некорректной реализации подсчета материального рейтинга
     * (даже MATERIAL_SIMPLE_ATTACK - это по сути решение основанное на глубине = 2,
     * а калькулятор рейтинга должен анализировать только ТЕКУЩУЮ ситуацию на доске в глубину 1!
     *
     * PS: так же с movesCount rating-ом возможно присутствует такая же проблема
     * по сути цепочка, на основе которой будет выбран analyzedMove такая
     * root -> maxB(analyzed) -> minP -> maxB -> minP -> maxB -> ...
     *
     * Т.е. если это ход бота (botLast() == true) - то нас интересует ход бота с наибольшим рейтингом (maxB),
     * но так же стоит учитывать ответ игрока, который тоже не дурак, а по сему ищем children.find(minP),
     * который в свою очередь должен учитывать ответ бота, который тоже в свою очередь не дурак: children.find(maxB)
     */
    public int getContextTotal() {
        if (getDeep() > App.MAX_DEEP) {
            return 0;
        }

        if (hasChildren()) {
            return getMoveTotal() + getMostPossibleChildContextTotal();
//            return getMoveTotal(!botLast()) + getMostPossibleChildContextTotal();
        }
        return getMoveTotal();

        //TODO: check by checkmate   -> context.isCheckmate() -> isCheckmateByBot/Player
        //TODO: deeper moves ratio? (without checkmate)
    }

    private int getMostPossibleChildContextTotal() {
        if (botLast()) {
            return getMinChildContextTotal();
        }
        else {
            return getMaxChildContextTotal();
        }
    }

    private int getMaxChildContextTotal() {
        return childrenStream().mapToInt(GameContext::getContextTotal).max().orElseThrow(() -> new CheckmateException(this));
    }

    private int getMinChildContextTotal() {
        return childrenStream().mapToInt(GameContext::getContextTotal).min().orElseThrow(() -> new CheckmateException(this));
    }

    public void print(int tabsCount, String prefix) {
        printMove(tabsCount, prefix);
        System.out.println("resultMove.context.total = " + getContextTotal());

        if (tabsCount == 0) {
//            getLastMove().printRating(tabsCount + 1);
        }

        System.out.println("--------------------------------");
        printMinMax(tabsCount + 1);
    }

    private void printMinMax(int tabsCount) {
        if (hasChildren()) {
            childrenStream()
                    .reduce((BinaryOperator.maxBy(Comparator.comparing(GameContext::getContextTotal))))
                    .ifPresent(maxContext -> {
                        maxContext.printMove(tabsCount, maxContext.getPrefix("max"), maxContext.getContextPostfix());
                        maxContext.printMinMax(tabsCount + 1);
                    });

            childrenStream()
                    .reduce((BinaryOperator.minBy(Comparator.comparing(GameContext::getContextTotal))))
                    .ifPresent(minContext -> {
                        minContext.printMove(tabsCount, minContext.getPrefix("min"), minContext.getContextPostfix());
                        minContext.printMinMax(tabsCount + 1);
                    });
        }
    }

    private void printMove(int tabsCount, String prefix) {
        printMove(tabsCount, prefix, "");
    }

    private void printMove(int tabsCount, String prefix, String postfix) {
        System.out.println(tabs(tabsCount) + prefix + "[" + lastMove + "][" + getDeep() + "].moveTotal = " + lastMove.getTotal() + ", ctxTotal = " + getContextTotal() + postfix);
        lastMove.printRating(tabsCount + 1);
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

    public void updateMaterialRatingRecursive(Function<GameContext, Rating> getRatingFunction) {
        if (hasChildren()) {
            childrenStream()
                    .forEach(childContext -> {
                        childContext.getLastMove().updateRating(getRatingFunction.apply(childContext));
                        childContext.updateMaterialRatingRecursive(getRatingFunction);
                    });
        }
    }
}
