package com.example.chess.service.impl.bot;

import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.Side;
import com.example.chess.service.support.*;
import com.example.chess.service.support.api.MoveHelperAPI;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        Side botSide = game.getActiveSide();
        Side playerSide = botSide.reverse();


        return move -> {
            MoveResult moveResult = matrix.executeMove(move.toMoveDTO(), null);
            CellsMatrix nextMatrix = moveResult.getNewMatrix();
            MoveHelperAPI nextMoveHelper = new MoveHelper(game, nextMatrix);

            Map<PointDTO, List<ExtendedMove>> playerNextStandardMovesMap = nextMoveHelper.getStandardMovesStream(playerSide)
                    .sorted(Comparator.comparingInt(ExtendedMove::getValueFrom))
                    .collect(Collectors.groupingBy(ExtendedMove::getPointTo));

            Map<PointDTO, List<ExtendedMove>> botNextDefensiveMovesMap = nextMoveHelper.getDefensiveMovesStream(botSide)
                    .sorted(Comparator.comparingInt(ExtendedMove::getValueFrom))
                    .collect(Collectors.groupingBy(ExtendedMove::getPointTo));


            if (move.isHarmful()) {
                List<ExtendedMove> playerNextStandardMoves = playerNextStandardMovesMap.get(move.getPointTo());
                List<ExtendedMove> botNextDefensiveMoves = botNextDefensiveMovesMap.get(move.getPointTo());
                botNextDefensiveMoves = botNextDefensiveMoves != null ? botNextDefensiveMoves : new ArrayList<>();

                if (playerNextStandardMoves == null) {
                    /*
                     * Если мы атакуем незащищенную фигуру противника, то это хороший ход.
                     * А незащищенная она потому, что после того как мы ее срубили -
                     * данную клетку не может атаковать ни одна фигура игрока
                     */
                    move.updateRatingByParam(RatingParam.ATTACK_TO_DEFENSELESS_PLAYER_PIECE, move.getValueTo());
                } else {
                    updateRatingByExchangeDiff(move, playerNextStandardMoves, botNextDefensiveMoves);
                }
            }


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

    private void updateRatingByExchangeDiff(ExtendedMove move, List<ExtendedMove> playerNextStandardMoves, List<ExtendedMove> botNextDefensiveMoves) {
        /*
         * Мы попали в этот блок, а значит что фигура (бота) срубившая фигуру игрока,
         * находится под атакой как минимум одной фигуры противника (игрока)
         */
        int botNextDefensiveMovesCount = botNextDefensiveMoves.size();
        int playerNextStandardMovesCount = playerNextStandardMoves.size();

        int exchangeDiff = move.getExchangeDiff();

        if (exchangeDiff > 0) {
            /*
             * Если мы срубили фигуру игрока более дешевой фигурой - то это тоже хороший ход.
             * И в данном случае можно уже дальше ничего не просчитывать.
             */
            move.updateRatingByParam(RatingParam.EXCHANGE_DIFF, exchangeDiff);
        } else {

            //Стоимость фигуры игрока, которую мы УЖЕ срубили
            int playerExpenses = move.getValueTo();
            //Стоимость фигуры бота, которая УЖЕ срубила фигуру игрока - при размене она 100% будет срублена
            int botExpenses = move.getValueFrom();

            //TODO: поидее здесь надо анализировать размен поэтапно, а не делать его до конца
            int countDiff = playerNextStandardMovesCount - botNextDefensiveMovesCount;
            if (countDiff > 0) {
                /*
                 * Мы попали в этот блок, а это значит, что мы срубили фигуру, которая ХОРОШО защищена.
                 * Т.е. если будем меняться до конца - игрок победит.
                 */
                //+стоимость фигур игрока, которые будут потеряны при дальнейшем размене (самая дорогая фигура останется жива)
                playerExpenses += getMovesSum(playerNextStandardMoves, ExtendedMove::getValueFrom, countDiff);
                //+стоимость фигур бота, которые будут потеряны при дальнейшем размене
                botExpenses += getMovesSum(botNextDefensiveMoves, ExtendedMove::getValueFrom);
            } else {
                /*
                 * Мы попали в этот блок, а это значит, что мы срубили фигуру, которая ПЛОХО защищена.
                 * Т.е. если будем меняться до конца - мы победим (бот то есть)
                 */
                //+стоимость фигур игрока, которые будут потеряны при дальнейшем размен
                playerExpenses += getMovesSum(playerNextStandardMoves, ExtendedMove::getValueFrom);
                //+стоимость фигур бота, которые будут потеряны при дальнейшем размене (самая дорогая фигура останется жива)
                botExpenses += getMovesSum(botNextDefensiveMoves, ExtendedMove::getValueFrom, Math.abs(countDiff) + 1);
            }

            int diff = playerExpenses - botExpenses;
            if (diff > 0) {
                /*
                 * Если мы здесь - значит размен нам (боту) выгоден в любом случае - значит это хороший ход.
                 */
                move.updateRatingByParam(RatingParam.EXCHANGE_DIFF, move.getValueTo());
            }
        }
    }

    /**
     * @param moves список, для которого будет подсчитана сумма. Внимание! Список должен быть отсортирован!
     * @param excludedElementsCount количество элементов с конца списка, для которых сумма не будет подсчитана
     */
    private int getMovesSum(List<ExtendedMove> moves, ToIntFunction<? super ExtendedMove> valueExtractor, int excludedElementsCount) {
        Objects.requireNonNull(moves);

        return IntStream.range(0, moves.size())
                .filter(i -> i < moves.size() - excludedElementsCount)  //самые дорогие элементы отфильтровываем
                .mapToObj(moves::get)
                .mapToInt(valueExtractor)
                .sum();
    }


    private int getMovesSum(List<ExtendedMove> moves, ToIntFunction<? super ExtendedMove> valueExtractor) {
        Objects.requireNonNull(moves);

        return moves.stream()
                .mapToInt(valueExtractor)
                .sum();
    }


    //    private void updateRatingByExchangeDiff(ExtendedMove move, List<ExtendedMove> playerNextStandardMoves, List<ExtendedMove> botNextDefensiveMoves) {
//        /*
//         * Мы попали в этот блок, а значит что фигура (бота) срубившая фигуру игрока,
//         * находится под атакой как минимум одной фигуры противника (игрока)
//         */
//        int botNextDefensiveMovesCount = botNextDefensiveMoves.size();
//        int playerNextStandardMovesCount = playerNextStandardMoves.size();
//
//        int exchangeDiff = move.getExchangeDiff();
//
//        if (exchangeDiff > 0) {
//            /*
//             * Если мы срубили фигуру игрока более дешевой фигурой - то это тоже хороший ход.
//             * И в данном случае можно уже дальше ничего не просчитывать.
//             */
//            move.updateRatingByParam(RatingParam.EXCHANGE_DIFF, exchangeDiff);
//        } else {
//            int playerExpenses;
//            int botExpenses;
//
//            if (playerNextStandardMovesCount > botNextDefensiveMovesCount) {
//                /*
//                 * Мы попали в этот блок, а это значит, что мы срубили фигуру, которая ХОРОШО защищена.
//                 * Т.е. если будем меняться до конца - игрок победит.
//                 */
//                playerExpenses =
//                        move.getValueTo()                                                                       //стоимость фигуры игрока, которую мы УЖЕ срубили
//                                + getMovesSum(playerNextStandardMoves, ExtendedMove::getValueFrom);  //стоимость фигур игрока, которые будут потеряны при дальнейшем размене (самая дорогая фигура останется жива)
//                botExpenses =
//                        move.getValueFrom()                                                                     //стоимость фигуры бота, которая УЖЕ срубила фигуру игрока - она будет срублена первой при размене
//                                + getMovesSum(botNextDefensiveMoves, ExtendedMove::getValueFrom);                //стоимость фигур бота, которые будут потеряны при дальнейшем размене
//            } else {
//                /*
//                 * Мы попали в этот блок, а это значит, что мы срубили фигуру, которая ПЛОХО защищена.
//                 * Т.е. если будем меняться до конца - мы победим (бот то есть)
//                 */
//                playerExpenses =
//                        move.getValueTo()                                                                   //стоимость фигуры игрока, которую мы УЖЕ срубили
//                                + getMovesSum(playerNextStandardMoves, ExtendedMove::getValueFrom);         //стоимость фигур игрока, которые будут потеряны при дальнейшем размен
//                botExpenses =
//                        move.getValueFrom()                                                                 //стоимость фигуры бота, которая УЖЕ срубила фигуру игрока - она будет срублена первой при размене
//                                + getMovesSum(botNextDefensiveMoves, ExtendedMove::getValueFrom); //стоимость фигур бота, которые будут потеряны при дальнейшем размене (самая дорогая фигура останется жива)
//
//            }
//
//            int diff = playerExpenses - botExpenses;
//            if (diff > 0) {
//                /*
//                 * Размен нам (боту) выгоден - значит это хороший ход.
//                 */
//                move.updateRatingByParam(RatingParam.EXCHANGE_DIFF, diff);
//            }
//        }
//    }
}
