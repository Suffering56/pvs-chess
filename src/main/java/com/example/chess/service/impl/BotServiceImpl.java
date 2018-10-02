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
import java.util.stream.Stream;

@Service
@Log4j2
public class BotServiceImpl implements BotService {

    private final GameService gameService;
    @Value("${app.game.bot.move-delay}")
    private Long botMoveDelay;

    private BotMode mode = BotMode.GREEDY;

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
                .peek(calculateRating(matrix))
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
        return MoveDTO.valueOf(resultMove.getPointFrom(), resultMove.getPointTo(), promotionPieceType);
    }

    private Consumer<? super ExtendedMove> calculateRating(CellsMatrix matrix) {
        switch (mode) {
            case DEVELOP:
                return updateRating(matrix);
            case GREEDY:
                return ExtendedMove::applyGreedyMode;
            case RANDOM:
            default:
                return extendedMove -> {
                };
        }
    }


    private Consumer<? super ExtendedMove> updateRating(CellsMatrix matrix) {
        return move -> {
            move.setRating(RatingParam.EXCHANGE_DIFF, move.getExchangeDiff());
//            move
        };
    }

    private ExtendedMove getRandomMove(List<ExtendedMove> movesList) {
        int i = (int) (movesList.size() * Math.random());
        return movesList.get(i);
    }
}
