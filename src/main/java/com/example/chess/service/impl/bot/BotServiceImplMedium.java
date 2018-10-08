package com.example.chess.service.impl.bot;

import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.Side;
import com.example.chess.service.support.*;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
@Log4j2
@Qualifier(BotMode.MEDIUM)
public class BotServiceImplMedium extends AbstractBotService {

    @Override
    protected MoveDTO findBestMove(Game game, CellsMatrix matrix) {
        return super.findBestMove(game, matrix);
    }

    @Override
    protected Consumer<? super ExtendedMove> calculateRating(Game game, CellsMatrix matrix) {
        return analyzedMove -> {
            MoveResult moveResult = matrix.executeMove(analyzedMove.toMoveDTO(), null);
            CellsMatrix nextMatrix = moveResult.getNewMatrix();

            Rating rating = getMaterialRating(analyzedMove, nextMatrix, game);
            analyzedMove.updateRating(rating);

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

    @SuppressWarnings("DanglingJavadoc")
    private Rating getMaterialRating(ExtendedMove analyzedMove, CellsMatrix newMatrix, Game game) {
        Side botSide = game.getActiveSide();
        Side playerSide = botSide.reverse();

        int targetCellValue = analyzedMove.getValueTo(0);                                                     //player
        PointDTO targetPoint = analyzedMove.getPointTo();                                                               //точка куда ходит move (здесь же происходит рубилово)


        int exchangeValue = targetCellValue;
        List<Integer> exchangeValues = new ArrayList<>();
        exchangeValues.add(exchangeValue);

        ExtendedMove minPlayerMove = getMinMove(game, newMatrix, targetPoint, playerSide);
        CellsMatrix beforePlayerMoveMatrix = newMatrix;

        while (true) {
            if (minPlayerMove == null) break;
            /**
             * EXECUTE PLAYER MOVE
             */
            CellsMatrix beforeBotMoveMatrix = beforePlayerMoveMatrix.executeMove(minPlayerMove.toMoveDTO(), null).getNewMatrix();
            ExtendedMove minBotMove = getMinMove(game, beforeBotMoveMatrix, targetPoint, botSide);

            exchangeValue -= minPlayerMove.getValueTo();
            exchangeValues.add(exchangeValue);

            if (minBotMove == null) break;
            /**
             * EXECUTE BOT MOVE
             */
            beforePlayerMoveMatrix = beforeBotMoveMatrix.executeMove(minBotMove.toMoveDTO(), null).getNewMatrix();
            minPlayerMove = getMinMove(game, beforePlayerMoveMatrix, targetPoint, playerSide);

            exchangeValue += minBotMove.getValueTo();
            exchangeValues.add(exchangeValue);
        }


        int exchangeDeep = exchangeValues.size();
        Rating.Builder builder = Rating.builder()
                .var("move", CommonUtils.moveToString(analyzedMove))
                .var("exchangeDeep", exchangeDeep);

        if (exchangeDeep == 1) {  //1) bot -> X
            if (targetCellValue == 0) {
                //diffList.get(0) == targetCellValue == 0  == пустая клетка (бот ничего не срубил)
                return builder.build(RatingParam.MATERIAL_SIMPLE_MOVE, exchangeValues.get(0));
            } else {
                //diffList.get(0) == targetCellValue == срубленная фигура
                return builder.build(RatingParam.MATERIAL_SIMPLE_FREEBIE, exchangeValues.get(0));
            }
        }
        if (exchangeDeep == 2) {  //1) bot -> X 2) player -> X
            if (targetCellValue == 0) {
                //diffList.get(1) == analyzedMove.getValueFrom() == minPlayerMove.getValueTo()  == отданная фигура
                return builder.build(RatingParam.MATERIAL_SIMPLE_FEED, exchangeValues.get(1));
            } else {
                //diffList.get(1) == targetCellValue - analyzedMove.getValueFrom() == срубленная фигура - отданная
                return builder.build(RatingParam.MATERIAL_SIMPLE_EXCHANGE, exchangeValues.get(1));
            }
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
                            .note("Math.min(totalMinB, maxP)")
                            .build(RatingParam.MATERIAL_DEEP_EXCHANGE, Math.min(totalMinB, maxP));
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
                            .note("Math.min(totalMaxP, minB)")
                            .build(RatingParam.MATERIAL_DEEP_EXCHANGE, Math.min(totalMaxP, minB));
                }
            }
        }

        throw new RuntimeException("WTF?");
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

    private void logMaterial(Object msg, Object... params) {
        log(LoggerParam.MATERIAL, msg, params);
    }
}
