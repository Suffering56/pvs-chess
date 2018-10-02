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
        MoveHelperAPI moveHelper = new MoveHelper(game, matrix);
        Side botSide = game.getPosition() % 2 == 0 ? Side.WHITE : Side.BLACK;

        List<ExtendedMove> allMovesList = moveHelper
                .getExtendedMovesStream(botSide)
                .peek(calculateRating(game, matrix, botSide))
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

        allMovesList.forEach(move -> log.info("\tmove[R:" + move.getTotal() + "]: " + CommonUtils.moveToString(move)));
        log.info("------------------------------------------------");
        log.info("allList.size = " + allMovesList.size());
        log.info("topList.size = " + topMovesList.size());
        log.info("resultMove: " + CommonUtils.moveToString(resultMove));
        log.info("============================================");

        return MoveDTO.valueOf(resultMove.getPointFrom(), resultMove.getPointTo(), promotionPieceType);
    }

    private Consumer<? super ExtendedMove> calculateRating(Game game, CellsMatrix matrix, Side botSide) {
        switch (mode) {
            case DEVELOP:
                return updateRating(game, matrix, botSide);
            case GREEDY:
                return ExtendedMove::applyGreedyMode;
            case RANDOM:
            default:
                return extendedMove -> {
                };
        }
    }


    private Consumer<? super ExtendedMove> updateRating(Game game, CellsMatrix matrix, Side botSide) {
        return move -> {
            move.setRating(RatingParam.EXCHANGE_DIFF, move.getExchangeDiff());
            move.setRating(RatingParam.ATTACK_DEFENSELESS_PIECE, getAttackDefenselessPieceValue(game, matrix, botSide, move));
        };
    }

    private int getAttackDefenselessPieceValue(Game game, CellsMatrix matrix, Side botSide, ExtendedMove move) {
        int value = 0;
        if (move.isBloody()) {
            //TODO: promotionPieceType can be not null
            MoveResult moveResult = matrix.executeMove(move.toMoveDTO(), null);
            MoveHelperAPI helper = new MoveHelper(game, moveResult.getNewMatrix());

            long count = helper.getExtendedMovesStream(botSide.reverse())
                    .filter(enemyMove -> enemyMove.getPointTo().equals(move.getPointTo()))
                    .count();

            if (count == 0) {
                value = move.getValueFrom();
            }
        }
        return value;
    }

    private ExtendedMove getRandomMove(List<ExtendedMove> movesList) {
        int i = (int) (movesList.size() * Math.random());
        return movesList.get(i);
    }
}
