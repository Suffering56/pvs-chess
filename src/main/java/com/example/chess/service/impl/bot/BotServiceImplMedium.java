package com.example.chess.service.impl.bot;

import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.Side;
import com.example.chess.service.support.*;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.util.Pair;
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
//        Side botSide = game.getActiveSide();
//
//        List<ExtendedMove> sortedBotMovesList = new MoveHelper(game, matrix)
//                .getStandardMovesStream(botSide)
//                .peek(calculateRating(game, matrix))
//                .sorted(Comparator.comparing(ExtendedMove::getTotal))
//                .collect(Collectors.toList());
//
//        int maxTotal = sortedBotMovesList.stream()
//                .mapToInt(ExtendedMove::getTotal)
//                .max()
//                .orElseThrow(() -> new RuntimeException("Checkmate!!!"));
//
//        List<ExtendedMove> topMovesList = sortedBotMovesList.stream()
//                .filter(extendedMove -> extendedMove.getTotal() == maxTotal)
//                .collect(Collectors.toList());
//
//        ExtendedMove resultMove = getRandomMove(topMovesList);
//
//
//        PieceType promotionPieceType = null;
//        if (resultMove.getPieceFrom() == PieceType.PAWN && (resultMove.getTo().getRowIndex() == 0 || resultMove.getTo().getRowIndex() == 7)) {
//            promotionPieceType = PieceType.QUEEN;
//        }
//
//        return MoveDTO.valueOf(resultMove.getPointFrom(), resultMove.getPointTo(), promotionPieceType);
        return super.findBestMove(game, matrix);
    }

    @Override
    protected Consumer<? super ExtendedMove> calculateRating(Game game, CellsMatrix matrix) {
        return analyzedMove -> {
            MoveResult moveResult = matrix.executeMove(analyzedMove.toMoveDTO(), null);
            CellsMatrix nextMatrix = moveResult.getNewMatrix();

            Pair<RatingParam, Integer> materialResult = updateRatingByMaterial(analyzedMove, nextMatrix, game);
            analyzedMove.updateRatingByParam(materialResult.getFirst(), materialResult.getSecond());

            /*
             * Здесь должны быть отрицательные рейтинги.
             *
             * TODO: а вдруг мат?
             * Например если после выполнения данного хода игрок сможет поставить мат следующим ходом.
             * Каким бы ни был хорошим этот ход - он резко превращается в очень плохой
             * Кроме случае если мы поставили мат. (в таком случае у игрока не будет доступных ходов,
             * а значит он не сможет поставить мат в ответ)
             *
             * TODO: а вдруг мы ходим связанной фигурой?
             * Или если после выполнения данного хода на доске оказалась наша БОЛЕЕ дорогая фигура под атакой.
             * Это может случиться если сделали ход связанной фигурой.
             */
        };
    }

    @SuppressWarnings("DanglingJavadoc")
    private Pair<RatingParam, Integer> updateRatingByMaterial(ExtendedMove analyzedMove, CellsMatrix newMatrix, Game game) {
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


        int movesCount = exchangeValues.size();
        log.info("movesCount[" + CommonUtils.moveToString(analyzedMove) + "] = " + movesCount);

        if (movesCount == 1) {  //1) bot -> X
            if (targetCellValue == 0) {
                return Pair.of(RatingParam.MATERIAL_SIMPLE_MOVE, exchangeValues.get(0));                                      //diffList.get(0) == targetCellValue == 0  == пустая клетка (бот ничего не срубил)
            } else {
                return Pair.of(RatingParam.MATERIAL_SIMPLE_FREEBIE, exchangeValues.get(0));                                   //diffList.get(0) == targetCellValue == срубленная фигура
            }
        }
        if (movesCount == 2) {  //1) bot -> X 2) player -> X
            if (targetCellValue == 0) {
                return Pair.of(RatingParam.MATERIAL_SIMPLE_FEED, exchangeValues.get(1));                                      //diffList.get(1) == analyzedMove.getValueFrom() == minPlayerMove.getValueTo()  == отданная фигура
            } else {
                return Pair.of(RatingParam.MATERIAL_SIMPLE_EXCHANGE, exchangeValues.get(1));                                  //diffList.get(1) == targetCellValue - analyzedMove.getValueFrom() == срубленная фигура - отданная
            }
        }
//        if (movesCount == 3) {
//            return Pair.of(RatingParam.MATERIAL_SIMPLE_EXCHANGE, Math.min(exchangeValues.get(0), exchangeValues.get(2)));
//        }

        int totalMinB = exchangeValues.get(0);
        int totalMaxP = exchangeValues.get(1);
        for (int n = 0; n < movesCount; n++) {
            Integer currentExchangeValue = exchangeValues.get(n);

            if (n % 2 == 0) {   //bot move
                totalMinB = Math.min(totalMinB, currentExchangeValue);
            } else {            //player move
                totalMaxP = Math.max(totalMaxP, currentExchangeValue);
            }
        }

        boolean botMovesLast = (movesCount - 1) % 2 == 0;
        int lastMoveValue = exchangeValues.get(movesCount - 1);
        if (botMovesLast) {
            totalMaxP = Math.max(totalMaxP, lastMoveValue);
        } else {    //playerMovesLast
            totalMinB = Math.min(totalMinB, lastMoveValue);
        }

        log.info("totalMaxP[" + CommonUtils.moveToString(analyzedMove) + "] = " + totalMaxP);
        log.info("totalMinB[" + CommonUtils.moveToString(analyzedMove) + "] = " + totalMinB);

        int minB = exchangeValues.get(0);
        int maxP = exchangeValues.get(1);
        for (int n = 0; n < movesCount; n++) {
            Integer currentExchangeValue = exchangeValues.get(n);

            boolean isLastMove = n == movesCount - 1;
            if (isLastMove && botMovesLast) {
                log.info("isLastMove && botMovesLast");
                return Pair.of(RatingParam.MATERIAL_DEEP_EXCHANGE, Math.min(totalMinB, totalMaxP));
            }

            //movesCount[Na6-x-c7

            if (n % 2 == 0) { //bot move
                if (currentExchangeValue == totalMinB) {
                    /* Stop by player:
                     * Игрок прекращает размен, потому что добрался до наиболее выгодного момента =>
                     * Когда БОТ сделал ход и после этого значение exchangeValue минимально:
                     * exchangeValue == totalMinB
                     *
                     * Т.к. первым прекратил размен игрок,
                     * Значит боту надо искать наиболее выгодный ход из тех что сделаны до этого хода
                     * В диапазоне: 0 <= X < n
                     */
                    log.info("maxP[" + CommonUtils.moveToString(analyzedMove) + "] = " + maxP);
                    log.info("minB[" + CommonUtils.moveToString(analyzedMove) + "] = " + minB);

                    if (n == 0) {
                        log.info("n == 0"); //TODO:
                        return Pair.of(RatingParam.MATERIAL_DEEP_EXCHANGE, minB);
                    }
                    return Pair.of(RatingParam.MATERIAL_DEEP_EXCHANGE, Math.min(minB, maxP));
                }

                minB = Math.min(minB, currentExchangeValue);
            } else { //player move
                maxP = Math.max(maxP, currentExchangeValue);

                if (currentExchangeValue == totalMaxP) {
                    /* Stop by bot:
                     * Бот прекращает размен, потому что добрался до наиболее выгодного момента =>
                     * Когда ИГРОК сделал ход и после этого значение exchangeValue максимально:
                     * exchangeValue == totalMaxP
                     *
                     * Т.к. первым прекратил размен бот,
                     * Значит бот надо искать наиболее выгодный ход из тех что сделаны до этого хода, ВКЛЮЧАЯ текущий
                     * В диапазоне: 0 <= X <= n
                     */
                    log.info("maxP[" + CommonUtils.moveToString(analyzedMove) + "] = " + maxP);
                    log.info("minB[" + CommonUtils.moveToString(analyzedMove) + "] = " + minB);
                    return Pair.of(RatingParam.MATERIAL_DEEP_EXCHANGE, Math.min(minB, maxP));
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

    //игрок должен отвечать - если diff после хода бота положительный
    //НО игрок должен остановиться на ходу n:
    // - если после текущего хода (n)
    // - бот сделает ход (n+1),
    // - потом игрок сделает ход(n+2)
    // => и если в этот момент diff(n+2) > diff (n)


    //bDiff - diff после хода бота
    //pDiff - diff после хода игрока


    //1)    бот делает ход (move - который мы оцениваем) = рубит фигуру
    //      старт цикла
    //2)    игрок отвечает (рубит фигуру бота наиболее дешевой)                                         //если наиболее дешевых ходов несколько - не имеет значения какая фигура срубит первой. невозможна ситуация, когда может случайно открыться шах и тд
    //diff update
    //3)    бот отвечает
    //diff update

    //бот ДОЛЖЕН остановиться тогда, когда на текущем ходу (n) когда bDiff(n) < bDiff(n + 2)            //фиксируем текущий diff
    //бот МОЖЕТ остановиться тогда, когда на текущем ходу (n) когда bDiff(n)  ==  bDiff(n + 2)          //фиксируем текущий diff

    //игрок ДОЛЖЕН остановиться тогда, когда на текущем ходу (n) если     bDiff(n + 1) > bDiff(n - 1)   //фиксируем diff (n-1)
    //игрок МОЖЕТ остановиться тогда, когда bDiff(n + 1)  ==  bDiff(n - 1)                              //фиксируем diff (n-1)


    //3)    теперь в цикле делаем по-очереди ходы.
    //      после каждой итерации делаем замер
    //3.1)  первым ходит бот
    //3.2)  затем игрок
    //


//    @SuppressWarnings("DanglingJavadoc")
//    private Pair<RatingParam, Integer> updateRatingByMaterial(ExtendedMove analyzedMove, CellsMatrix newMatrix, Game game) {
//        Side botSide = game.getActiveSide();
//        Side playerSide = botSide.reverse();
//
//        int targetCellValue = analyzedMove.getValueTo(0);                                                     //player
//        PointDTO targetPoint = analyzedMove.getPointTo();                                                               //точка куда ходит move (здесь же происходит рубилово)
//
//
//        int currentDiff = targetCellValue;
//        List<Integer> diffList = new ArrayList<>();
//        diffList.add(currentDiff);
//
//        ExtendedMove minPlayerMove = getMinMove(game, newMatrix, targetPoint, playerSide);
//        CellsMatrix beforePlayerMoveMatrix = newMatrix;
//
//        /*
//         * Позиция на доске = matrix(n).
//         * n = 1/3/5/7/9 - это позиция на доске, образовавшаяся после выполнения хода игрока (следующим должен ходить бот)
//         * n = 0/2/4/6/8 - это позиция на доске, образовавшаяся после выполнения хода бота (следующим должен ходить игрок)
//         *
//         * matrix(n=0) - это позиция на доске, образовавшаяся после выполнения analyzedMove
//         */
//        int n = 0;
//        while (true) {
//            boolean isPlayerCanContinue = true;
//
//            if (currentDiff < 0) {
//                /*
//                 * Бот срубил на одну фигуру больше, но при этом остался в минусе.  (TODO: MATERIAL_SIMPLE_FEED)
//                 * Например: (5x1 => 1x5 => Xx5). diff = -4;
//                 * Игрок может дальше ходить как угодно - он уже в плюсе.
//                 * Значит дальнейший размен бессмысленен.
//                 * Значит надо выходить из цикла и давать материальную оценку ходу.
//                 *
//                 * К этой ситуации привел предыдущий ход бота move(n-2)
//                 * Если код заглянул в этот блок, значит n>=2
//                 * Потому что diff не может быть отрицательным после первого хода бота
//                 */
//
//                if (n == 2) {
//                    /*
//                     * Если n-2==0, значит предыдущий ход(n-2) - это analyzedMove.
//                     * TODO: pos: F
//                     */
//                    return Pair.of(RatingParam.MATERIAL_DEEP_EXCHANGE_LAST_WORD_FOR_BOT, diffList.get(n - 1));               //TODO: fix result
//                }
//            } else {
//                if (currentDiff > 0) {
//                    /*
//                     * Бот срубил на одну фигуру больше и при этом остался в плюсе.
//                     * Игрок должен продолжить размен, если конечно это не приведет к еще бОльшему ухудшению ситуации
//                     */
//                } else {  //diff = 0
//                    //либо бот шагнул на пустую клетку - тут игрок может как атаковать, так и сделать любой другой ход(n == 2)
//                    //либо произошел равноценный размен (3x3 -> Xx3)
//                }
//            }
//
//            if (n >= 2 && currentDiff - diffList.get(n - 2) < 0) {
//                /*
//                 * Если diff изменился в худшую сторону, значит не нужно было продолжать размен.
//                 * Значит предыдущий ход бота не нужно было делать
//                 */
//                return Pair.of(RatingParam.MATERIAL_DEEP_EXCHANGE_LAST_WORD_FOR_BOT, diffList.get(n - 2));               //TODO: fix result
//            }
//
//
//            if (minPlayerMove != null && isPlayerCanContinue) {
//                /**
//                 * EXECUTE PLAYER MOVE
//                 */
//                CellsMatrix beforeBotMoveMatrix = beforePlayerMoveMatrix.executeMove(minPlayerMove.toMoveDTO(), null).getNewMatrix();
//                ExtendedMove minBotMove = getMinMove(game, beforeBotMoveMatrix, targetPoint, botSide);
//
//                n++;
//                currentDiff -= minPlayerMove.getValueTo();
//                diffList.add(currentDiff);
//
//
//                boolean isBotCanContinue = true;    //TODO
//                if (minBotMove != null && isBotCanContinue) {   //нужно научить останавливаться
//                    /**
//                     * EXECUTE BOT MOVE
//                     */
//                    beforePlayerMoveMatrix = beforeBotMoveMatrix.executeMove(minBotMove.toMoveDTO(), null).getNewMatrix();
//                    minPlayerMove = getMinMove(game, beforePlayerMoveMatrix, targetPoint, playerSide);
//
//                    n++;
//                    currentDiff += minBotMove.getValueTo();
//                    diffList.add(currentDiff);
//                } else {
//                    //TODO: pos: B - first  (1x1)
//                    //TODO: pos: D - other  (2x2+)
//                    return Pair.of(RatingParam.MATERIAL_DEEP_EXCHANGE_LAST_WORD_FOR_PLAYER, currentDiff);               //но с обеих сторон срубили равное количество фигур
//
//                }
//            } else {
//                //TODO: pos: E  1x0
//                //TODO: pos: C  2x1
//                return Pair.of(RatingParam.MATERIAL_DEEP_EXCHANGE_LAST_WORD_FOR_BOT, currentDiff);                      //бот срубил на однфу фигуру больше
//            }
//        }
//
//    }


//    /*
//     * Позиция на доске = matrix(n).
//     * matrix(n=0) - это позиция на доске, образовавшаяся после выполнения analyzedMove
//     *
//     * n == 0 - бот сделал ход      (analyzedMove)
//     * n == 1 - игрок сделал ход
//     * n == 2 - бот сделал ход
//     * n == 3 - игрок делает ход...
//     */
//        for (int n = 3; n < movesCount; n++) {
//        Side side = n % 2 == 0 ? botSide : playerSide;          //сторона, которая только что сделала свой ход (УЖЕ СДЕЛАЛА)
//        boolean isPenultimateMove = n + 2 == movesCount;        //предпоследний ход
//        boolean isLastMove = n + 2 == movesCount;               //последний ход
//
//        int currentExchangeValue = exchangeValues.get(n);
//        int currentDiff = exchangeValues.get(n) - exchangeValues.get(n - 2);
//
//        if (botMovesLast) {
//            int resultExchangeValue = maxExchangeValueAfterPlayerMove >= exchangeValues.get(n - 2) ? maxExchangeValueAfterPlayerMove : exchangeValues.get(n - 2);
//            if (currentDiff > prevDiff) {
//                return Pair.of(RatingParam.MATERIAL_DEEP_EXCHANGE, resultExchangeValue);
//            }
//        } else {    //playerMovesLast
//            if (currentDiff < prevDiff) {
//                return Pair.of(RatingParam.MATERIAL_DEEP_EXCHANGE, exchangeValues.get(n - 2));
//            }
//        }
//
//        prevDiff = currentDiff;
//        if (side == playerSide && maxExchangeValueAfterPlayerMove < currentExchangeValue) {
//            maxExchangeValueAfterPlayerMove = currentExchangeValue;
//        }
//    }
}
