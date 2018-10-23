package com.example.chess.service.impl.bot;

import com.example.chess.ChessConstants;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.KingNotFoundException;
import com.example.chess.exceptions.UnattainablePointException;
import com.example.chess.service.support.*;
import com.example.chess.utils.CommonUtils;
import lombok.Getter;
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
    protected Consumer<? super ExtendedMove> calculateRating(FakeGame fakeGame, CellsMatrix originalMatrix, Side botSide, boolean isExternalCall) {

        List<ExtendedMove> botMovesByOriginal = MoveHelper.valueOf(fakeGame, originalMatrix)
                .getStandardMovesStream(botSide)
                .collect(Collectors.toList());
        /*
         * 0) matrix = originalMatrixBotNext;
         *
         * 1) executing bot move...
         * matrix = firstMatrixPlayerNext
         * 2) executing player move...
         * matrix = firstMatrixPlayerNextBotNext
         * 3) executing bot move...
         */
        return analyzedMove -> {
            InternalBotService internalService = new InternalBotService(fakeGame, originalMatrix, analyzedMove, botSide);
            CellsMatrix firstMatrixPlayerNext = internalService.getFirstMatrixPlayerNext();

            Rating materialRating = internalService.getMaterialRating(firstMatrixPlayerNext, analyzedMove, botSide, -1);
            analyzedMove.updateRating(materialRating);

            Rating invertedMaterialRating = internalService.getInvertedMaterialRating(-1);
            analyzedMove.updateRating(invertedMaterialRating);

            Rating checkRating = internalService.getCheckRating();
            analyzedMove.updateRating(checkRating);

            Rating movesCountRating = internalService.getAvailableMovesCountRating(botMovesByOriginal, botSide, false);
            analyzedMove.updateRating(movesCountRating);

            Rating invertedMovesCountRating = internalService.getInvertedAvailableMovesCountRating();
            analyzedMove.updateRating(invertedMovesCountRating);


            /*
             * TODO:
             * 1) оценить каждый ответ бота, найти самый лучший (maxTotal)
             * 2) Map<PlayerMove, List<BotMoves>> ----> Map<PlayerMove, BotMoves.maxTotal> -----> tempMap
             * 3) AnalyzedMove -> min(tempMap.values)
             * 4) put into rating (DEEP_ANALYSIS)
             */
//            на каждый ход противника -> список всех доступных для бота вариантов ответа
            try {
                if (isExternalCall) {
                    int val = internalService.getPlayerMoves()
                            .parallelStream()
                            .filter(move -> move.hasDifferentPointTo(analyzedMove))
                            .map(move -> {
                                CellsMatrix secondMatrixBotNext = firstMatrixPlayerNext.executeMove(move.toMoveDTO(), null).getNewMatrix();
                                ExtendedMove bestExtendedMove = findBestExtendedMove(fakeGame, secondMatrixBotNext, botSide, false);
                                if (bestExtendedMove != null) {
                                    return bestExtendedMove.getTotal();
                                }
                                return -ChessConstants.CHECKMATE_VALUE;
                            })
                            .mapToInt(Integer::intValue)
                            .min().orElse(0);

                    analyzedMove.updateRating(Rating.builder().build(RatingParam.DEEP, val));
                }
            } catch (KingNotFoundException e) {
                e.print();
                System.exit(0);
            }


            //pawn promotion
            //fork (вилка)
            //pin (связка)
            //sacrifice
            //hidden check

            /*
             * Здесь должны быть отрицательные рейтинги.
             * TODO: а вдруг мат?
             * Например если после выполнения данного хода игрок сможет поставить мат следующим ходом.
             * Каким бы ни был хорошим этот ход - он резко превращается в очень плохой
             * Кроме случае если мы поставили мат. (в таком случае у игрока не будет доступных ходов,
             * а значит он не сможет поставить мат в ответ)
             *
             */
        };
    }

    //TODO:originalMatrix->analyzedMove->firstMatrixPlayerNext>playerMove->secondMatrixBotNext->botMove->....
    class InternalBotService {

        @Getter
        private final CellsMatrix firstMatrixPlayerNext;
        private final FakeGame originalFakeGame;
        private final FakeGame fakeGame;
        private final CellsMatrix originalMatrix;
        @Getter
        private final List<ExtendedMove> playerMoves;
        private final ExtendedMove analyzedMove;
        private final Side botSide;
        private final Side playerSide;
        private final List<ExtendedMove> playerMovesByOriginal;


        public InternalBotService(FakeGame originalFakeGame, CellsMatrix originalMatrix, ExtendedMove analyzedMove, Side botSide) {
            this.analyzedMove = analyzedMove;
            this.originalFakeGame = originalFakeGame;
            this.fakeGame = originalFakeGame;
            this.originalMatrix = originalMatrix;


            this.botSide = botSide;
            playerSide = botSide.reverse();

            firstMatrixPlayerNext = originalMatrix.executeMove(analyzedMove.toMoveDTO(), null).getNewMatrix();

            playerMoves = MoveHelper.valueOf(originalFakeGame, firstMatrixPlayerNext)
                    .getStandardMovesStream(playerSide)
                    .collect(Collectors.toList());

            playerMovesByOriginal = MoveHelper.valueOf(fakeGame, originalMatrix)
                    .getStandardMovesStream(playerSide)
                    .collect(Collectors.toList());
        }

        /**
         * Считает количество ходов expectedSide до хода бота и после.
         * Разницу записывает в рейтинг. diff = movesAfter.size - movesBefore.size
         * expectedSide - может быть как ботом так и игроком.
         * <p>
         * Если expectedSide == botSide:
         * - боту выгодно, если сделанный ход дает ЕМУ БОЛЬШЕ пространства (возможностей) на доске.
         * - значит боту выгодно когда diff > 0; (значит что текущий ход позволяет боту на следующий ход получить бОльшую вариацию ходов)
         * <p>
         * Если expectedSide == playerSide:
         * - боту выгодно, если сделанный ход дает ИГРОКУ МЕНЬШЕ пространства.
         * - значит боту выгодно когда diff < 0;
         * <p>
         * FIXME: в этом рейтинге есть косяки:
         * 1) Он заставляет ферзя рано ходить. Т.к. ферзь в центре - приведет к максимальному коэффициенту.
         * 2) Он пытается убрать пешки с вертикалей A,H чтобы как можно раньше вывести ладьи (тем самым исключая собственную рокировку)
         * 3) Можно попасть под троекратное повторение одной и той же позиции (было такое, правда до внедрения INVERTED_AVAILABLE_MOVES_COUNT)
         */
        private Rating getAvailableMovesCountRating(List<ExtendedMove> movesBefore, Side expectedSide, boolean isInverted) {
            movesBefore = movesBefore.stream()
                    .filter(move -> move.getPieceFrom() != PieceType.KING)
                    .collect(Collectors.toList());

            List<ExtendedMove> movesAfter = MoveHelper.valueOf(fakeGame, firstMatrixPlayerNext)
                    .getStandardMovesStream(expectedSide)
                    .filter(move -> move.getPieceFrom() != PieceType.KING)
                    .collect(Collectors.toList());

            Rating.Builder builder = Rating.builder()
                    .var("movesBefore", movesBefore.size())
                    .var("movesAfter", movesAfter.size());

            if (isInverted) {
                return builder.build(RatingParam.INVERTED_AVAILABLE_MOVES_COUNT, movesAfter.size() - movesBefore.size());
            } else {
                return builder.build(RatingParam.AVAILABLE_MOVES_COUNT, movesAfter.size() - movesBefore.size());
            }
        }

        private Rating getInvertedAvailableMovesCountRating() {
            return getAvailableMovesCountRating(playerMovesByOriginal, playerSide, true);
        }

        /**
         * Алгоритм работает не только для определения материального рейтинга для бота, но и для игрока так же.
         * Достаточно ему передать в параметр botSide - playerSide.
         * В таком случае все понятия внутри функций инвертируются - все что было ботом - станет игроком, и наоборот.
         * <p>
         * Некрасиво, но если переименовать botSide и playerSide во что-то более абстрактное - легко будет запутаться.
         * <p>
         * ...->moveBefore->matrix->...
         */
        private Rating getMaterialRating(CellsMatrix matrix, ExtendedMove moveBefore, Side side, int maxDeep) {
            List<Integer> exchangeValues = generateExchangeValuesList(matrix, moveBefore, side, maxDeep);

            int exchangeDeep = exchangeValues.size();
            Rating.Builder builder = Rating.builder()
                    .var("exchangeDeep", exchangeDeep);

            if (exchangeDeep <= 2) {
                return getMaterialRatingForSimpleMoves(builder, exchangeValues);
            } else {
                return getMaterialRatingForDeepExchange(builder, exchangeValues);
            }
        }

        /**
         * Алгоритм анализирует ситуацию на доске образовавшуюся после того как бот сделал текущий ход (analyzedMove)
         * Он ищет все доступные враждебные(которые ведут к потере ботом фигуры или размену) ходы врага,
         * кроме тех, которые хотят срубить только что сходившую фигуру бота (эти размены анализируются в другом месте)
         * И анализирует этот самый размен вплоть до максимальной глубины.
         * Если показатель положительный (для игрока), значит текущий сделанный ход - плохой и получит отрицательный рейтинг.
         */
        private Rating getInvertedMaterialRating(int maxDeep) {
            List<ExtendedMove> playerHarmfulMoves = playerMoves
                    .stream()
                    .filter(move -> move.isHarmful() && move.hasDifferentPointTo(analyzedMove))
                    .collect(Collectors.toList());

            Rating.Builder builder = Rating.builder();
            int maxPlayerMoveValue = 0;

            for (ExtendedMove playerMove : playerHarmfulMoves) {
                CellsMatrix secondMatrixBotNext = firstMatrixPlayerNext.executeMove(playerMove.toMoveDTO(), null).getNewMatrix();

                Rating playerMoveRating = getMaterialRating(secondMatrixBotNext, playerMove, playerSide, maxDeep);

                String varName = "INVERTED_" + playerMoveRating.getParam() + "[" + CommonUtils.moveToString(playerMove) + "]";
                builder.var(varName, playerMoveRating.getValue());

                maxPlayerMoveValue = Math.max(maxPlayerMoveValue, playerMoveRating.getValue());
            }

            return builder.build(RatingParam.INVERTED_MATERIAL_FOR_PLAYER, maxPlayerMoveValue);
        }

        private Rating getCheckRating() {
            if (MoveHelper.valueOf(fakeGame, firstMatrixPlayerNext).isKingUnderAttack(playerSide)) {

                if (playerMoves.isEmpty()) {
                    return Rating.builder().build(RatingParam.CHECKMATE_BY_BOT);   ////TODO: checkmate
                }
                return Rating.builder().build(RatingParam.CHECK);
            }

            return Rating.builder().build(RatingParam.CHECK, 0);
        }

        private List<Integer> generateExchangeValuesList(CellsMatrix matrix, ExtendedMove moveBefore, Side botSide, int maxDeep) {
            Side playerSide = botSide.reverse();

            int targetCellValue = moveBefore.getValueTo(0);
            PointDTO targetPoint = moveBefore.getPointTo();

            List<Integer> exchangeValues = new ArrayList<>();

            int exchangeValue = targetCellValue;
            exchangeValues.add(exchangeValue);

            CellsMatrix prevMatrix = matrix;
            ExtendedMove prevMove = moveBefore;

            try {
                ExtendedMove minPlayerMove = getMinMove(fakeGame, matrix, targetPoint, playerSide);
                CellsMatrix afterBotMoveMatrix = matrix;


                while (true) {
                    if (minPlayerMove == null || exchangeValues.size() == maxDeep) break;
                    /*
                     * EXECUTE PLAYER MOVE
                     */
                    CellsMatrix afterPlayerMoveMatrix = afterBotMoveMatrix.executeMove(minPlayerMove.toMoveDTO(), null).getNewMatrix();
                    ExtendedMove minBotMove = getMinMove(fakeGame, afterPlayerMoveMatrix, targetPoint, botSide);
                    prevMatrix = afterBotMoveMatrix;
                    prevMove = minPlayerMove;

                    exchangeValue -= minPlayerMove.getValueTo();
                    exchangeValues.add(exchangeValue);

                    if (minBotMove == null || exchangeValues.size() == maxDeep) break;
                    /*
                     * EXECUTE BOT MOVE
                     */
                    afterBotMoveMatrix = afterPlayerMoveMatrix.executeMove(minBotMove.toMoveDTO(), null).getNewMatrix();
                    minPlayerMove = getMinMove(fakeGame, afterBotMoveMatrix, targetPoint, playerSide);
                    prevMatrix = afterPlayerMoveMatrix;
                    prevMove = minBotMove;

                    exchangeValue += minBotMove.getValueTo();
                    exchangeValues.add(exchangeValue);
                }
            } catch (KingNotFoundException e) {
                e.setOriginalMatrix(originalMatrix);
                e.setAnalyzedMove(analyzedMove);
                e.setPrevMatrix(prevMatrix);
                e.setPrevMove(prevMove);
                e.print();
                throw e;
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
            throw new UnattainablePointException();
        }

        //TODO: имеет значение чем рубить - если фигуры одинаковой стоимости (min) - скрытый шах или скрытая атака на более дорогую фигуру или наоборот одна из фигур бота связана с более дорогой фигурой
        //TODO: а еще про fakeGame подумай (longPawn, under check)
        private ExtendedMove getMinMove(FakeGame fakeGame, CellsMatrix matrix, PointDTO targetPoint, Side side) {
            return MoveHelper.valueOf(fakeGame, matrix)
                    .getStandardMovesStream(side)
                    .filter(nextMove -> nextMove.hasSamePointTo(targetPoint))
                    .reduce((m1, m2) -> m1.getValueFrom() <= m2.getValueFrom() ? m1 : m2)
                    .orElse(null);
        }

        private boolean isBotMove(int n) {
            return n % 2 == 0;
        }
    }

}


