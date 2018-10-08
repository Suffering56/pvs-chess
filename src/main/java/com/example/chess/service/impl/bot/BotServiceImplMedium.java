package com.example.chess.service.impl.bot;

import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.UnattainablePointException;
import com.example.chess.service.support.*;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Log4j2
@Qualifier(BotMode.MEDIUM)
public class BotServiceImplMedium extends AbstractBotService {

    @Override
    protected Consumer<? super ExtendedMove> calculateRating(Game game, CellsMatrix originalMatrix) {
        return analyzedMove -> {
            Side botSide = game.getActiveSide();
            Side playerSide = botSide.reverse();

            MoveResult moveResult = originalMatrix.executeMove(analyzedMove.toMoveDTO(), null);
            CellsMatrix nextMatrix = moveResult.getNewMatrix();

            Rating materialRating = getMaterialRating(game, nextMatrix, analyzedMove, botSide, -1);
            analyzedMove.updateRating(materialRating);

            Rating invertedMaterialRating = getInvertedMaterialRating(game, nextMatrix, analyzedMove, playerSide);
            analyzedMove.updateRating(invertedMaterialRating);

            /*
             * Здесь должны быть отрицательные рейтинги.
             * TODO: спасение фигуры
             * Мы ходим одно фигурой, а у нас в это время под атакой другая:
             * в EASY_MODE - я давал положительный рейтинг за спасение - это неправильно
             * нужно давать отрицательный рейтинг за НЕспасение
             *
             * TODO: а вдруг мы ходим связанной фигурой?
             * Или если после выполнения данного хода на доске оказалась наша БОЛЕЕ дорогая фигура под атакой.
             * Это может случиться если сделали ход связанной фигурой.
             *
             * TODO: а вдруг мат?
             * Например если после выполнения данного хода игрок сможет поставить мат следующим ходом.
             * Каким бы ни был хорошим этот ход - он резко превращается в очень плохой
             * Кроме случае если мы поставили мат. (в таком случае у игрока не будет доступных ходов,
             * а значит он не сможет поставить мат в ответ)
             *
             */
        };
    }

    private Rating getInvertedMaterialRating(Game game, CellsMatrix nextMatrix, ExtendedMove analyzedMove, Side playerSide) {
        List<ExtendedMove> playerHarmfulMoves = new MoveHelper(game, nextMatrix)
                .getStandardMovesStream(playerSide)
                .filter(move -> move.isHarmful() && move.hasDifferentPointTo(analyzedMove))
                .collect(Collectors.toList());

        Rating.Builder builder = Rating.builder();
        int maxPlayerMoveValue = 0;

        for (ExtendedMove playerMove : playerHarmfulMoves) {
            CellsMatrix afterPlayerMoveMatrix = nextMatrix.executeMove(playerMove.toMoveDTO(), null).getNewMatrix();

            Rating playerMoveRating = getMaterialRating(game, afterPlayerMoveMatrix, playerMove, playerSide, -1);

            String varName = "INVERTED_" + playerMoveRating.getParam() + "[" + CommonUtils.moveToString(playerMove) + "]";
            builder.var(varName, playerMoveRating.getValue());

            maxPlayerMoveValue = Math.max(maxPlayerMoveValue, playerMoveRating.getValue());
        }

        return builder.build(RatingParam.INVERTED_MATERIAL_FOR_PLAYER, maxPlayerMoveValue);
    }

    private Rating getMaterialRating(Game game, CellsMatrix newMatrix, ExtendedMove analyzedMove, Side botSide, int maxDeep) {
        List<Integer> exchangeValues = generateExchangeValuesList(game, newMatrix, analyzedMove, botSide, maxDeep);

        int exchangeDeep = exchangeValues.size();
        Rating.Builder builder = Rating.builder()
                .var("exchangeDeep", exchangeDeep);

        if (exchangeDeep <= 2) {
            return getMaterialRatingForSimpleMoves(builder, exchangeValues);
        } else {
            return getMaterialRatingForDeepExchange(builder, exchangeValues);
        }
    }

    private List<Integer> generateExchangeValuesList(Game game, CellsMatrix afterFirstMoveMatrix, ExtendedMove alreadyExecutedMove, Side botSide, int maxDeep) {
        Side playerSide = botSide.reverse();

        int targetCellValue = alreadyExecutedMove.getValueTo(0);
        PointDTO targetPoint = alreadyExecutedMove.getPointTo();

        List<Integer> exchangeValues = new ArrayList<>();

        int exchangeValue = targetCellValue;
        exchangeValues.add(exchangeValue);

        ExtendedMove minPlayerMove = getMinMove(game, afterFirstMoveMatrix, targetPoint, playerSide);
        CellsMatrix afterBotMoveMatrix = afterFirstMoveMatrix;

        while (true) {
            if (minPlayerMove == null || exchangeValues.size() == maxDeep) break;
            /*
             * EXECUTE PLAYER MOVE
             */
            CellsMatrix afterPlayerMoveMatrix = afterBotMoveMatrix.executeMove(minPlayerMove.toMoveDTO(), null).getNewMatrix();
            ExtendedMove minBotMove = getMinMove(game, afterPlayerMoveMatrix, targetPoint, botSide);

            exchangeValue -= minPlayerMove.getValueTo();
            exchangeValues.add(exchangeValue);

            if (minBotMove == null || exchangeValues.size() == maxDeep) break;
            /*
             * EXECUTE BOT MOVE
             */
            afterBotMoveMatrix = afterPlayerMoveMatrix.executeMove(minBotMove.toMoveDTO(), null).getNewMatrix();
            minPlayerMove = getMinMove(game, afterBotMoveMatrix, targetPoint, playerSide);

            exchangeValue += minBotMove.getValueTo();
            exchangeValues.add(exchangeValue);
        }

        return exchangeValues;
    }

    private Rating getMaterialRatingForSimpleMoves(Rating.Builder builder, List<Integer> exchangeValues) {
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

        throw new RuntimeException("exchangeDeep > 2");
    }

    private Rating getMaterialRatingForDeepExchange(Rating.Builder builder, List<Integer> exchangeValues) {
        int exchangeDeep = exchangeValues.size();

        if (exchangeDeep < 3) {
            throw new RuntimeException("exchangeDeep < 3");
        }

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
                     * - ессно он выбирает меньшее из двух зол == Math.min(totalMinB, maxP);
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
        throw new UnattainablePointException();
    }

    //TODO: имеет значение чем рубить - если фигуры одинаковой стоимости (min) - скрытый шах или скрытая атака на более дорогую фигуру или наоборот одна из фигур бота связана с более дорогой фигурой
    //TODO: а еще про game подумай (longPawn, under check)
    private ExtendedMove getMinMove(Game game, CellsMatrix matrix, PointDTO targetPoint, Side side) {
        return new MoveHelper(game, matrix)
                .getStandardMovesStream(side)
                .filter(nextMove -> nextMove.getPointTo().equals(targetPoint))
                .reduce((m1, m2) -> m1.getValueFrom() <= m2.getValueFrom() ? m1 : m2)
                .orElse(null);
    }

    private boolean isBotMove(int n) {
        return n % 2 == 0;
    }
}
