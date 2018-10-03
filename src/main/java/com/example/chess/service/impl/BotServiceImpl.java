package com.example.chess.service.impl;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.*;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import com.example.chess.service.support.*;
import com.example.chess.service.support.api.MoveHelperAPI;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Log4j2
public class BotServiceImpl implements BotService {

    private final GameService gameService;
    @Value("${app.game.bot.move-delay}")
    private Long botMoveDelay;

    private BotMode mode = BotMode.DEVELOP;

    public BotServiceImpl(GameService gameService) {
        this.gameService = gameService;
    }

    @Profile
    @Override
    public void applyBotMove(Game game) {
        CommonUtils.executeInSecondaryThread(() -> {
            CellsMatrix cellsMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());
            MoveDTO moveDTO = findBestMove(game, cellsMatrix);
            gameService.applyMove(game, moveDTO);
        });
    }

    private MoveDTO findBestMove(Game game, CellsMatrix matrix) {
        Side botSide = game.getActiveSide();

        List<ExtendedMove> allMovesList = new MoveHelper(game, matrix)
                .getAvailableExtendedMovesStream(botSide)
                .peek(calculateRating(game, matrix))
                .sorted(Comparator.comparing(ExtendedMove::getTotal))
                .collect(Collectors.toList());

        int maxTotal = allMovesList.stream()
                .mapToInt(ExtendedMove::getTotal)
                .max()
                .orElseThrow(() -> new RuntimeException("Checkmate!!!"));

        List<ExtendedMove> topMovesList = allMovesList.stream()
                .filter(extendedMove -> extendedMove.getTotal() == maxTotal)
                .collect(Collectors.toList());

        ExtendedMove resultMove = getRandomMove(topMovesList);


        PieceType promotionPieceType = null;
        if (resultMove.getPieceFrom() == PieceType.PAWN && (resultMove.getTo().getRowIndex() == 0 || resultMove.getTo().getRowIndex() == 7)) {
            promotionPieceType = PieceType.QUEEN;
        }

        log.info("\n============================================");
        allMovesList.forEach(move -> log.info("\tmove[R:" + move.getTotal() + "]: " + CommonUtils.moveToString(move)));
        log.info("------------------------------------------------");
        log.info("allList.size = " + allMovesList.size());
        log.info("topList.size = " + topMovesList.size());
        log.info("resultMove[" + (matrix.getPosition() + 1) + "]: " + CommonUtils.moveToString(resultMove));


        return MoveDTO.valueOf(resultMove.getPointFrom(), resultMove.getPointTo(), promotionPieceType);
    }

    private Consumer<? super ExtendedMove> calculateRating(Game game, CellsMatrix matrix) {
        switch (mode) {
            case DEVELOP:
                return updateRating(game, matrix);
            case GREEDY:
                return ExtendedMove::applyGreedyMode;
            case RANDOM:
            default:
                return extendedMove -> {
                };
        }
    }

    private Consumer<? super ExtendedMove> updateRating(Game game, CellsMatrix matrix) {
        log.info("updateRating");

        Side botSide = game.getActiveSide();
        Side playerSide = botSide.reverse();
        MoveHelper moveHelper = new MoveHelper(game, matrix);

        //key -> cellTo, value = bot moves
        Map<CellDTO, List<ExtendedMove>> allBotMovesMap = new MoveHelper(game, matrix)
                .getAvailableExtendedMovesStream(botSide)
                .collect(Collectors.groupingBy(ExtendedMove::getTo));

        //key -> cellTo, value = player moves
        Map<CellDTO, List<ExtendedMove>> passivePlayerMovesMap = moveHelper
                .getAvailableExtendedMovesStream(playerSide)
                .filter(ExtendedMove::isNotBloody)
                .collect(Collectors.groupingBy(ExtendedMove::getTo));

        //key -> protectedCell, value = defensive bot moves
        Map<CellDTO, List<ExtendedMove>> defensiveBotMovesMap = moveHelper
                .getDefensiveMovesStream(botSide)
                .collect(Collectors.groupingBy(ExtendedMove::getTo));

        //key -> victimCell, value = attacking moves
        Map<CellDTO, List<ExtendedMove>> attackingPlayerMovesMap = moveHelper
                .getAttackingMovesStream(playerSide)
                .collect(Collectors.groupingBy(ExtendedMove::getTo));

        printMap(defensiveBotMovesMap, "protectedCell");
        log.info("\n\n");
        printMap(attackingPlayerMovesMap, "victimCell");


        Set<CellDTO> victimCells = new HashSet<>();
        attackingPlayerMovesMap.forEach((victimCell, attackingMoves) -> {
            List<ExtendedMove> defensiveMoves = defensiveBotMovesMap.get(victimCell);

            int defensiveMovesCount = 0;
            if (defensiveMoves != null) {
                defensiveMovesCount = defensiveMoves.size();
            }

            if (attackingMoves.size() - defensiveMovesCount > 0) {
                victimCells.add(victimCell);
            }
        });


        return move -> {
            //TODO: promotionPieceType can be not null
            MoveResult moveResult = matrix.executeMove(move.toMoveDTO(), null);
            MoveHelperAPI nextMoveHelper = new MoveHelper(game, moveResult.getNewMatrix());

            move.updateRatingByParam(RatingParam.EXCHANGE_DIFF, move.getExchangeDiff());
            move.updateRatingByParam(RatingParam.ATTACK_DEFENSELESS_PIECE, getAttackDefenselessPieceValue(nextMoveHelper, botSide, move));

            if (nextMoveHelper.isKingUnderAttack(playerSide)) {
                move.updateRatingByParam(RatingParam.CHECK);

                long availablePlayerMovesCount = nextMoveHelper.getAvailableExtendedMovesStream(playerSide).count();
                if (availablePlayerMovesCount == 0) {
                    move.updateRatingByParam(RatingParam.CHECKMATE);
                }
            }

            if (victimCells.contains(move.getFrom())) {
                move.updateRatingByParam(RatingParam.RESCUE, move.getValueFrom());
            }

            List<ExtendedMove> passivePlayerMoves = passivePlayerMovesMap.get(move.getTo());
            if (passivePlayerMoves != null) {
                List<ExtendedMove> defensiveBotMoves = allBotMovesMap.get(move.getTo());

                /*
                TODO: суть в том, что defensiveBotMoves содержат в себе ходы пешек, которые на самом то деле вовсе ничего не защищают
                TODO: а так же НЕ содержат атакующие ходы пешек, потому что на момент получения ходов, пешкам рубить было нечего
                 */
                defensiveBotMoves = defensiveBotMoves.stream()
                        .filter(m -> {
                            if (m.getPieceFrom() == PieceType.PAWN) {
                                if (m.getFrom().getColumnIndex().equals(m.getTo().getColumnIndex())) {
                                    return false;
                                }
                            }
                            return true;
                        }).collect(Collectors.toList());

                log.info("\t\t\tdefensiveMovesFor: " + move.getTo());
                for (ExtendedMove botMove : defensiveBotMoves) {
                    log.info("\t\t\tdefensiveMove: " + CommonUtils.moveToString(botMove));
                }
                int defensiveMovesCount = defensiveBotMoves.size() - 1;

                if (passivePlayerMoves.size() - defensiveMovesCount > 0) {
                    move.updateRatingByParam(RatingParam.LOSS, move.getValueFrom());
                } else {
                    for (ExtendedMove passivePlayerMove : passivePlayerMoves) {
                        if (passivePlayerMove.getValueFrom() < move.getValueFrom()) {
                            move.updateRatingByParam(RatingParam.LOSS, move.getValueFrom());
                        }
                    }
                }
            }
        };
    }

    //TODO: botSide не обязательно передавать. есть же newMatrix.getPosition()
    //TODO: мне кажется примерно таким же способом можно реализовать
    private Integer getAttackDefenselessPieceValue(MoveHelperAPI moveHelper, Side botSide, ExtendedMove move) {
        if (move.isBloody()) {
            long count = moveHelper.getAvailableExtendedMovesStream(botSide.reverse())
                    .filter(enemyMove -> enemyMove.getPointTo().equals(move.getPointTo()))
                    .count();

            if (count == 0) {
                return move.getValueFrom();
            }
        }
        return null;
    }

    private ExtendedMove getRandomMove(List<ExtendedMove> movesList) {
        int i = (int) (movesList.size() * Math.random());
        return movesList.get(i);
    }

    private void printMap(Map<CellDTO, List<ExtendedMove>> map, String cellName) {
        map.forEach((cell, extendedMoves) -> {
            log.info(cellName + ": " + cell);
            extendedMoves.forEach(move -> log.info(CommonUtils.moveToString(move)));
        });
    }
}
