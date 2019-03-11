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
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
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
//    Map<PointDTO, List<GameContext>> children;  //возможно стоит поменять на array[][][]
    Set<GameContext> children;

    @Setter boolean isCheckmate;

    public void fill(int maxDeep) {
        fill(maxDeep, move -> true);
    }

    /**
     * Заполняет детей по targetPoint-у.
     * Представим ситуацию что Kf3 (sourcePoint) срубил пешку на e5 (targetPoint)
     * В таком случае нужно проверить - а не рубит ли никто нашего коня, который теперь на e5.
     * Если рубит - то надо так же проверить - а не можем ли мы отомстить за нашего коня (ищем фигуры которые могут атаковать e5)
     * В общем - по сути метод ищет все фигуры которые атакуют e5 и заполняет их в children
     *
     * Как эффект у данного контекста будет несколько вариантов развития событий (чем больше фигур могут рубить тем больше вариантов):
     * WF1 - белая фигура #1, которая может рубить на e5
     * WF2 - белая фигура #2 которая так же атакует e5
     * BF0 - черная фигура которую неизбежно срубят, т.к. она уже стоит на клетке,
     * BF1 - черная фигура, которая защищает фигуру BF0 на e5 и может отомстить белым, срубив фигуру срубившую BF0.
     *
     * 1) 1: WF1-x-e5(BF0); 2: BF1-x-e5(WF1); 3: WF2-x-e5(BF1)
     * 2) 1: WF2-x-e5(BF0); 2: BF1-x-e5(WF2); 3: WF1-x-e5(BF1)
     */
    public void fillForExchange(PointDTO targetPoint) {
        fill(Integer.MAX_VALUE, childMove -> childMove.hasSamePointTo(targetPoint));
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
        //TODO: Perhaps, it need to sync
        if (children == null) {
            children = new HashSet<>();
        }
        children.add(childNode);
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

    private boolean botLast() {
        return root == null || root.getBotSide() == lastMoveSide();
    }

    private boolean isPositiveMove() {
        return botLast();
    }

    private boolean isNegativeMove() {
        return !botLast();
    }

    private int getMoveSignum() {
        return isPositiveMove() ? 1 : -1;
    }

    //TODO: private int chainTotal;

    /**
     * Допустим есть цепочка таких ходов (все ходы имеют один и тот же targetPoint)
     * 1) Ход бота:     черный слон рубит белого коня (+300)
     * 2) Ход игрока:   белая ладья рубит черного слона (+300)
     * 3) Ход бота:     черная пешка рубит белую ладью (+500)
     * 4) Ход игрока:   белая любая фигура рубит черную пешку (+100)
     *
     * origin(0) -> +300 -> +300 -> +500 -> +100
     *
     * Для нее функция вернет такой результат: 0 + 300 - 300 + 500 - 100 =  +400
     *
     * Это значение показывает выгодность данного хода для того, чей ход идет сразу же после RootMatrix
     * (по сути это выгода для бота. но если введу анализ ходов игрока, то все инвертируется)
     *
     * Ну и да - отрицательное значение покажет что данная цепочка ходов ни к чему хорошему не приведет
     *
     * PS: ответ на вопрос - включительно или нет? Да включительно! Т.е. тотал lastMove текущего контекста учтен
     */
    public int getTotalOfChain() {
        Preconditions.checkState(getDeep() >= App.MAX_DEEP);

        int result = 0;
        GameContext context = this;

        while (!context.isRoot()) {
            result += context.getMoveSignum() * context.getLastMove().getValueTo(0);
            context = context.getParent();
        }

        return result;
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

    @Nonnull
    public Stream<GameContext> childrenStream() {
        Preconditions.checkNotNull(children);
        return children.stream();
    }

    public Stream<GameContext> childrenStream(@Nonnull PointDTO targetPoint) {
        Preconditions.checkNotNull(targetPoint);

        return childrenStream()
                .filter(context -> targetPoint.equals(context.getLastMove().getPointTo()));
    }

    public Stream<GameContext> neighboursStream() {
        Preconditions.checkNotNull(parent);

        return parent.childrenStream()
                .filter(context -> context != this);
    }

    //TODO: private final int deep;
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

    boolean stopped;

    public void markStopped() {
        //TODO: нужно разделить на паблик и приватный, в котором уже будет не ==, а getDeep() > MAX_DEEP
        Preconditions.checkState(getDeep() == App.MAX_DEEP);

        int initialTotal = getTotalOfChain();

        if (hasChildren()) {
            //раз мы здесь, значит оппонент может срубить context.lastMove().getPieceFrom();

//            childrenStream()
//                    .forEach(context -> {   //context = playerContext
//
//                        int currentTotal = context.getTotalOfChain();
//                        int prevTotal = context.getParent().getTotalOfChain();
//
//                        context.markStopped();
//                    });
        }
    }

//    private Stream<GameContext> deepestChildren() {
//        //по сути это обход дерева в ширину
//        if (!hasChildren()) {
//            return parent.childrenStream();
//        }
//
//
//
//        throw new UnsupportedOperationException();
//    }

    public void consumeLeafs(Consumer<GameContext> consumer) {
        if (!hasChildren()) {
            consumer.accept(this);
        }
        else {
            childrenStream()
                    .forEach(context -> context.consumeLeafs(consumer));
        }
    }
}
