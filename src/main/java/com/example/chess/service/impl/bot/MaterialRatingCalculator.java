package com.example.chess.service.impl.bot;

import com.example.chess.dto.PointDTO;
import com.example.chess.enums.RatingParam;
import com.example.chess.exceptions.CheckmateException;
import com.example.chess.logic.objects.Rating;
import com.example.chess.logic.objects.game.GameContext;
import com.example.chess.logic.objects.move.ExtendedMove;
import com.google.common.base.Preconditions;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"Duplicates", "WeakerAccess"})
@UtilityClass
public class MaterialRatingCalculator {

    private static final int MAX_MATERIAL_DEEP = -1;

    public static Rating getMaterialRating(GameContext gameContext, boolean isInverted) throws CheckmateException {
        List<Integer> exchangeValues = generateExchangeValuesList(gameContext, isInverted);

        int exchangeDeep = exchangeValues.size();
        Rating.Builder builder = Rating.builder()
                .var("exchangeDeep", exchangeDeep);

        if (exchangeDeep <= 2) {
            return getMaterialRatingForSimpleMoves(builder, exchangeValues);
        } else {
            return getMaterialRatingForDeepExchange(builder, exchangeValues);
        }
    }

    public static Rating getInvertedMaterialRating(GameContext gameContext) throws CheckmateException {
        Rating.Builder builder = Rating.builder();

        MutableInt maxPlayerMoveValue = new MutableInt(0);
        MutableObject<GameContext> maxContext = new MutableObject<>(null);

        if (gameContext.hasChildren()) {
            gameContext.childrenStream()
                    .filter(context -> context.getLastMove().isHarmful() && context.getLastMove().hasDifferentPointTo(gameContext.getLastMove()))
                    .forEach(context -> {
                        Rating tempRating = getMaterialRating(context, true);

                        if (tempRating.getValue() > maxPlayerMoveValue.getValue()) {
                            maxContext.setValue(context);
                            maxPlayerMoveValue.setValue(tempRating.getValue());
                        }
                    });
        }

        if (maxContext.getValue() != null) {
            builder.reasonMove(maxContext.getValue().getLastMove());
        }
        return builder.build(RatingParam.INVERTED_MATERIAL_FOR_PLAYER, maxPlayerMoveValue.getValue());
    }

    /**
     * До того как я придумал GameContext этот метод был в 2 раза длиннее и непонятнее.
     * В общем он собирает данные о размене фигур, который может произойти в точке, куда пошел бот (или игрок если isInverted = true)
     * Размен происходит "до талого" пока у одной из сторон не кончатся фигуры которые вообще могут пойти в эту точку.
     * <p>
     * Но это даже не обязательно будет размен, это может быть обычный ход на никем не атакуемую клетку.
     * Для простых разменов есть упрощенная реализация подсчета рейтинга: getMaterialRatingForSimpleMoves()
     * Для сложных - getMaterialRatingForDeepExchange()
     */
    private static List<Integer> generateExchangeValuesList(GameContext gameContext, boolean isInverted) throws CheckmateException {
        PointDTO targetPoint = gameContext.getLastMove().getPointTo();
        List<Integer> exchangeValuesResult = new ArrayList<>();

        int exchangeValue = 0;
        GameContext deepContext = gameContext;

        /*
         * inverted=false botLast=true          +
         * inverted=false botLast=false         -
         * inverted=false botLast=true          +
         * inverted=false botLast=false         -

         * inverted=true botLast=false          +
         * inverted=true botLast=true           -
         * inverted=true botLast=false          +
         * inverted=true botLast=true           -
         *
         * В общем исходя из этих данных вытекает это выражение: isInverted != deepContext.botLast()
         */

        //noinspection ConstantConditions
        do {
//            deepContext.setDeepExchangeAlreadyCalculated(true);
            ExtendedMove lastMove = deepContext.getLastMove();

            if (isInverted != deepContext.botLast()) {
                exchangeValue += lastMove.getValueTo(0);
            } else {
                exchangeValue -= lastMove.getValueTo(0);
            }

//            if (lastMove.getSide() == initialSide) {
//                exchangeValue += lastMove.getValueTo(0);
//            } else {
//                exchangeValue -= lastMove.getValueTo(0);
//            }

            exchangeValuesResult.add(exchangeValue);

            deepContext = findDeeperCheapestContext(deepContext, targetPoint);
        }
        while (deepContext != null && exchangeValuesResult.size() != MAX_MATERIAL_DEEP);

        return exchangeValuesResult;
    }

    private static GameContext findDeeperCheapestContext(GameContext context, PointDTO targetPoint) throws CheckmateException {
        if (!context.hasChildren()) {
            context.fill(1, childMove -> childMove.getPointTo().equals(targetPoint));
        }

        if (!context.hasChildren()) {
            if (context.getDeep() == 1) {
                throw new CheckmateException(context);
            } else {
                //TODO: handle deepest Checkmates
                return null;
            }
        }

        return context.childrenStream(targetPoint)
                // TODO: имеет значение чем рубить если фигуры одинаковой стоимости (т.е. тупо reduce-ить = плохо)
                //  - скрытый шах или скрытая атака на более дорогую фигуру или наоборот одна из фигур бота связана с более дорогой фигурой
                .reduce((c1, c2) -> c1.getLastMove().getValueFrom() <= c2.getLastMove().getValueFrom() ? c1 : c2)
                .orElse(null);
    }

    private static Rating getMaterialRatingForSimpleMoves(Rating.Builder builder, List<Integer> exchangeValues) {
        int exchangeDeep = exchangeValues.size();

        if (exchangeDeep == 1) {  //1) bot -> X
            if (exchangeValues.get(0) == 0) {
                //бот шагнул на незащищенную (ботом), но безопасную (не находящуюся под атакой игрока) клетку = сделал самый обычный ход
                return builder.build(RatingParam.MATERIAL_SIMPLE_MOVE, exchangeValues.get(0));
            } else {
                //бот срубил незащищенную фигуру игрока
                return builder.build(RatingParam.MATERIAL_SIMPLE_FREEBIE, exchangeValues.get(0));
            }
        }
        if (exchangeDeep == 2) {  //1) bot -> X 2) player -> X
            if (exchangeValues.get(0) == 0) {

                //бот шагнул на незащищенную (ботом) пустую клетку, находящуюся под атакой игрока = отдал фигуру
                return builder.build(RatingParam.MATERIAL_SIMPLE_FEED, exchangeValues.get(1));
            } else {
                //бот срубил фигуру, но срубившая фигура ничем теперь не защищена и игрок может ее срубить = простой размен
                return builder.build(RatingParam.MATERIAL_SIMPLE_EXCHANGE, exchangeValues.get(1));
            }
        }

        throw new UnsupportedOperationException("exchangeDeep > 2");
    }

    /**
     * Жуткий метод.
     * Помню придумывал его несколько дней и закончил в 5 утра.
     * <p>
     * В общем там алгоритм пытается найти точку, в которой размен прекратится.
     * Это будет максимально выгодная и для игрока и для бота точка.
     * Понятно что по сути для них максимально выгодные точки будут разными, но дело в том что ходы последовательны,
     * а значит кто-то достигнув своей максимально выгодной точки прекратит дальнейший размен.
     * Но тут нюанс - раз размен прекратился раньше чем если бы все рубились до победного,
     * то возможно той стороне, которая не дошла до своей точки теперь стоит прекратить размен еще раньше...
     * Ну в общем и так далее.
     * <p>
     * Я надеюсь этот алгоритм как раз и делает то, что описано выше и в итоге находит точку, которая устраивает обоих,
     * и уже на ее основе производит подсчет рейтинга.
     */
    private static Rating getMaterialRatingForDeepExchange(Rating.Builder builder, List<Integer> exchangeValues) {
        int exchangeDeep = exchangeValues.size();
        Preconditions.checkState(exchangeDeep >= 3, "exchangeDeep must be >= 3");

        int totalMinB = exchangeValues.get(0);
        int totalMaxP = exchangeValues.get(1);
        for (int n = 0; n < exchangeDeep; n++) {
            Integer currentExchangeValue = exchangeValues.get(n);

            if (isBotMove(n)) {   //bot move
                totalMinB = Math.min(totalMinB, currentExchangeValue);
            } else {            //player move
                totalMaxP = Math.max(totalMaxP, currentExchangeValue);
            }
        }

        boolean botMovesLast = isBotMove(exchangeDeep - 1);
        int lastMoveValue = exchangeValues.get(exchangeDeep - 1);
        if (botMovesLast) {
            totalMaxP = Math.max(totalMaxP, lastMoveValue);
        } else {//playerMovesLast
            totalMinB = Math.min(totalMinB, lastMoveValue);
        }

        builder.var("totalMaxP", totalMaxP);
        builder.var("totalMinB", totalMinB);

        int minB = exchangeValues.get(0);
        int maxP = exchangeValues.get(1);   //на самом деле null, но я не хочу использовать Integer;
        for (int n = 0; n < exchangeDeep; n++) {
            Integer currentExchangeValue = exchangeValues.get(n);

            boolean stopByPlayer = currentExchangeValue == totalMinB;
            boolean stopByBot = currentExchangeValue == totalMaxP;

            builder.var("n", n);

            if (isBotMove(n)) {//уже сделанный
                minB = Math.min(minB, currentExchangeValue);

                if (stopByPlayer) {
                    builder.note("stopByPlayer");

                    /*
                     * Игрок прекращает размен, потому что добрался до наиболее выгодного для сеья момента =>
                     * Поэтому именно бот выбирает:
                     * - либо последнее слово будет за ним - т.к. дальше игрок не пойдет и result = totalMinB,
                     * - либо последнее слово будет за игроком, а значит result = maxP
                     * - ессно он выбирает меньшее из двух зол == Math.max(totalMinB, maxP) ;
                     */

                    if (n == 0) {
                        /*
                         * Такое может произойти, в случае если игрок добрался до наиболее выгодного для себя момента первым же ходом.
                         * Бот должен выбрать меньшее из двух зол (Math.min(totalMinB, maxP))
                         * Но maxP в текущий момент скажем... null.
                         * По коду не скажешь, но на самом деле дело до вычисления корректного значения maxP не доходит.
                         *
                         * Поэтому возвращаем min(totalMinB, null) = totalMinB;
                         */
                        return builder
                                .var("maxP", null)
                                .note("return totalMinB")
                                .build(RatingParam.MATERIAL_DEEP_EXCHANGE, totalMinB);
                    }
                    return builder
                            .var("maxP", maxP)
                            .var("minB", minB)
                            .note("return Math.max(totalMinB, maxP)")
                            .build(RatingParam.MATERIAL_DEEP_EXCHANGE, Math.max(totalMinB, maxP));
                }
            } else { //player move
                builder.note("stopByBot");
                maxP = Math.max(maxP, currentExchangeValue);
                if (stopByBot) {
                    /*
                     * Бот прекращает размен, потому что добрался до наиболее выгодного для себя момента =>
                     * Поэтому именно игрок выбирает:
                     * - либо последнее слово будет за ним - т.к. дальше бот не пойдет и result = totalMaxP,
                     * - либо последнее слово будет за ботом, а значит result = minB
                     * - ессно он выбирает меньшее из двух зол == Math.min(totalMaxP, minB);
                     */
                    return builder
                            .var("maxP", maxP)
                            .var("minB", minB)
                            .note("return Math.min(totalMaxP, minB)")
                            .build(RatingParam.MATERIAL_DEEP_EXCHANGE, Math.min(totalMaxP, minB));
                }
            }
        }

        //ситуации, когда код доберется сюда быть не должно. по крайней мере я такую придумать не смог
        throw new UnsupportedOperationException();
    }

    private static boolean isBotMove(int n) {
        return n % 2 == 0;
    }
}
