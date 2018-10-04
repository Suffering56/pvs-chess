package com.example.chess.service.impl.bot;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.support.*;
import com.example.chess.service.support.api.MoveHelperAPI;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Log4j2
@Service
@Qualifier(BotMode.EASY)
public class BotServiceImplEasy extends AbstractBotService {

    @Profile
    @Override
    public void applyBotMove(Game game) {
        CommonUtils.executeInSecondaryThread(() -> {
            CellsMatrix cellsMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());
            MoveDTO moveDTO = findBestMove(game, cellsMatrix);
            gameService.applyMove(game, moveDTO);
        });
    }

    protected Consumer<? super ExtendedMove> calculateRating(Game game, CellsMatrix matrix) {
        log.info("updateRating");

        Side botSide = game.getActiveSide();
        Side playerSide = botSide.reverse();
        MoveHelper moveHelper = new MoveHelper(game, matrix);

        /*
            В полученной коллекции собраны ходы бота, которые в текущий момент нацелены на пустую клетку.
            Но в будущем, пригодятся для возможности поставить свою фигуру под защиту от другой фигуры.

            Поэтому в botPossibleMovesMap есть только диагональные ходы пешек (пешка не может защитить фигуру,
            если находится с ней на одной вертикали.

            Итого:
                - мы взяли все доступые ходы на текущий момент
                - убрали все ходы для пешек
                - добавили диагональные ходы для пешек
                - вуаля! получилась botPossibleMovesMap;
         */
        //key -> cellTo, value = bot possible moves
        Map<CellDTO, List<ExtendedMove>> botPossibleMovesMap = moveHelper
                .getPossibleMovesStream(botSide)
                .collect(Collectors.groupingBy(ExtendedMove::getTo));

        //key -> protectedCell, value = defensive bot moves
        Map<CellDTO, List<ExtendedMove>> botDefensiveMovesMap = moveHelper
                .getDefensiveMovesStream(botSide)
                .collect(Collectors.groupingBy(ExtendedMove::getTo));

        //key -> victimCell, value = player harmful moves
        Map<CellDTO, List<ExtendedMove>> playerHarmfulMovesMap = moveHelper
                .getStandardMovesStream(playerSide)
                .filter(ExtendedMove::isHarmful)
                .collect(Collectors.groupingBy(ExtendedMove::getTo));


//        printMap(botDefensiveMovesMap, "protectedCell");
//        log.info("\n\n");
//        printMap(playerHarmfulMovesMap, "victimCell");
//        log.info("\n\n");
//        printMap(botPossibleMovesMap, "botPossibleMove");
//        log.info("\n\n");

        Set<CellDTO> victimCells = getVictimCells(botDefensiveMovesMap, playerHarmfulMovesMap);


        return move -> {
            log.info("handleMove: " + CommonUtils.moveToString(move));
            log.info("cellFrom: " + move.getFrom());
            log.info("cellTo: " + move.getTo());

            //TODO: promotionPieceType can be not null
            MoveResult moveResult = matrix.executeMove(move.toMoveDTO(), null);
            CellsMatrix nextMatrix = moveResult.getNewMatrix();
            MoveHelperAPI nextMoveHelper = new MoveHelper(game, nextMatrix);

            //EXCHANGE_DIFF
            move.updateRatingByParam(RatingParam.EXCHANGE_DIFF, move.getExchangeDiff());

            //ATTACK_TO_DEFENSELESS_PLAYER_PIECE
            if (isAttackToDefenseless(nextMoveHelper, playerSide, move)) {
                move.updateRatingByParam(RatingParam.ATTACK_TO_DEFENSELESS_PLAYER_PIECE, move.getValueFrom());
            }

            //CHECK
            if (nextMoveHelper.isKingUnderAttack(playerSide)) {
                move.updateRatingByParam(RatingParam.CHECK);

                //CHECKMATE
                long availablePlayerMovesCount = nextMoveHelper.getStandardMovesStream(playerSide).count();
                if (availablePlayerMovesCount == 0) {
                    move.updateRatingByParam(RatingParam.CHECKMATE);
                }
            }

            //SAVE_BOT_PIECE
            if (victimCells.contains(move.getFrom())) {
                move.updateRatingByParam(RatingParam.SAVE_BOT_PIECE, move.getValueFrom());
            }

            //USELESS_VICTIM
            //key -> cellTo, value = player possible moves
            Map<CellDTO, List<ExtendedMove>> nextPlayerHarmfulMovesMap = nextMoveHelper
                    .getStandardMovesStream(playerSide)
                    .filter(ExtendedMove::isHarmful)
                    .collect(Collectors.groupingBy(ExtendedMove::getTo));

//            printMap(nextPlayerHarmfulMovesMap, "nextPlayerHarmfulMove");


            CellDTO nextMoveCellTo = nextMatrix.getCell(move.getPointTo());
            List<ExtendedMove> playerMoves = nextPlayerHarmfulMovesMap.get(nextMoveCellTo);
            if (playerMoves != null) {
//                log.info("playerMoves != null");

                if (isAttackedByCheaperPiece(move.getFrom(), playerMoves)) {
//                    log.info("isAttackedByCheaperPiece(move.getFrom(), playerMoves)");
                    /*
                        Если мы ставим фигуру под атаку вражеской более дешевой фигуры
                        То в 99% случаев это очень плохой ход
                        TODO: sacrifice/depth
                     */
                    move.updateRatingByParam(RatingParam.USELESS_VICTIM, move.getValueFrom());

                } else {
//                    log.info("else");
                    List<ExtendedMove> botPossibleMoves = botPossibleMovesMap.get(move.getTo());

                    if (botPossibleMoves == null) {
//                        log.info("botPossibleMoves == null");
                        if (move.getPieceFrom() != PieceType.PAWN) {
                            throw new RuntimeException("move.from.getPiece must be PAWN only!");
                        }

                        /*
                            Такая ситуация может произойти, только если мы пошли пешкой.
                            При этом мы поставили ее на клетку, которую никто не защищает.
                            Но поскольку мы внутри (if playerHarmlessMoves != null), значит эта клетка под атакой.
                            Значит мы просто решили отдать пешку противнику нахаляву
                         */
                        move.updateRatingByParam(RatingParam.USELESS_VICTIM, move.getValueFrom());
                    } else {
//                        log.info("botPossibleMoves == null ==>>> else");
                        /*
                            subtrahend - вычитаемое. Дело в том, что botDefensiveMovesByCellTo содержит в том числе
                            и текущий ход, который нужно вычесть, т.к. он уже ничего защищать не будет если его сделать.
                            НО! это правило работает всегда КРОМЕ случаев когда мы делаем ход пешкой (без взятия).
                            такие ходы отсутствуют в botDefensiveMovesByCellTo - поэтому вычитать ничего не нужно
                        */
                        int subtrahend = 1;
                        if (move.getPieceFrom() == PieceType.PAWN && move.isVertical()) {
                            subtrahend = 0;
                        }

//                        log.info("playerMoves.size: " + playerMoves.size());
//                        log.info("botPossibleMoves.size: " + botPossibleMoves.size());
//                        log.info("subtrahend: " + subtrahend);

                        /*
                            Если мы ставим фигуру на клетку, которая находится под атакой игрока.
                            И при этом количество атакующих фигур (игрока) превышает количество защищающих (бота)
                            То скорее всего это будет так же плохой ход
                            TODO: Есть исключения (например: если нашего слона защищенного пешкой атакует ферзь и ладья - то все норм)
                         */
                        if (playerMoves.size() > botPossibleMoves.size() - subtrahend) {
                            move.updateRatingByParam(RatingParam.USELESS_VICTIM, move.getValueFrom());
                        }
                    }
                }
            } else {
//                log.info("playerMoves == null");
            }
        };
    }

    private Set<CellDTO> getVictimCells(Map<CellDTO, List<ExtendedMove>> botDefensiveMovesMap, Map<CellDTO, List<ExtendedMove>> playerHarmfulMovesMap) {
        Set<CellDTO> victimCells = new HashSet<>();

        playerHarmfulMovesMap.forEach((victimCell, playerHarmfulMoves) -> {
            List<ExtendedMove> defensiveMoves = botDefensiveMovesMap.get(victimCell);

            int defensiveMovesCount = 0;
            if (defensiveMoves != null) {
                defensiveMovesCount = defensiveMoves.size();
            }

            if (isAttackedByCheaperPiece(victimCell, playerHarmfulMoves)) {
                victimCells.add(victimCell);
            } else if (playerHarmfulMoves.size() > defensiveMovesCount) {
                victimCells.add(victimCell);
            }
        });
        return victimCells;
    }

    //TODO: botSide не обязательно передавать. есть же newMatrix.getPosition()
    //TODO: мне кажется примерно таким же способом можно реализовать
    private boolean isAttackToDefenseless(MoveHelperAPI nextMoveHelper, Side playerSide, ExtendedMove move) {
        if (move.isHarmful()) {
            long count = nextMoveHelper.getStandardMovesStream(playerSide)
                    .filter(enemyMove -> enemyMove.getPointTo().equals(move.getPointTo()))
                    .count();

            return count == 0;
        }
        return false;
    }

}
