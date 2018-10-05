package com.example.chess.service.impl.bot;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import com.example.chess.service.support.*;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Log4j2
public abstract class AbstractBotService implements BotService {

    protected GameService gameService;
    protected Long botMoveDelay;

    @Profile
    @Override
    public void applyBotMove(Game game) {
        CommonUtils.executeInSecondaryThread(() -> {
            CellsMatrix cellsMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());
            MoveDTO moveDTO = findBestMove(game, cellsMatrix);
            gameService.applyMove(game, moveDTO);
        });
    }

    protected abstract Consumer<? super ExtendedMove> calculateRating(Game game, CellsMatrix matrix);

    protected MoveDTO findBestMove(Game game, CellsMatrix matrix) {
        Side botSide = game.getActiveSide();

        List<ExtendedMove> sortedBotMovesList = new MoveHelper(game, matrix)
                .getStandardMovesStream(botSide)
                .peek(calculateRating(game, matrix))
                .sorted(Comparator.comparing(ExtendedMove::getTotal))
                .collect(Collectors.toList());

        int maxTotal = sortedBotMovesList.stream()
                .mapToInt(ExtendedMove::getTotal)
                .max()
                .orElseThrow(() -> new RuntimeException("Checkmate!!!"));

        List<ExtendedMove> topMovesList = sortedBotMovesList.stream()
                .filter(extendedMove -> extendedMove.getTotal() == maxTotal)
                .collect(Collectors.toList());

        ExtendedMove resultMove = getRandomMove(topMovesList);


        PieceType promotionPieceType = null;
        if (resultMove.getPieceFrom() == PieceType.PAWN && (resultMove.getTo().getRowIndex() == 0 || resultMove.getTo().getRowIndex() == 7)) {
            promotionPieceType = PieceType.QUEEN;
        }

        sortedBotMovesList.forEach(move -> {
            log.info("\tmove[R:" + move.getTotal() + "]: " + CommonUtils.moveToString(move));
            if (move.getTotal() != 0) {
                move.getRatingMap().forEach((ratingParam, value) -> log.info("\tRP: " + ratingParam + "=>" + value));
            }
        });
        log.info("------------------------------------------------");
        log.info("sortedList.size = " + sortedBotMovesList.size());
        log.info("topList.size = " + topMovesList.size());
        log.info("resultMove[" + (matrix.getPosition() + 1) + "]: " + CommonUtils.moveToString(resultMove));
        resultMove.getRatingMap().forEach((ratingParam, value) -> log.info("\tRP: " + ratingParam + "=>" + value));

        log.info("\n============================================");
        log.info("\n============================================");
        log.info("\n============================================");

        return MoveDTO.valueOf(resultMove.getPointFrom(), resultMove.getPointTo(), promotionPieceType);
    }

    protected ExtendedMove getRandomMove(List<ExtendedMove> movesList) {
        int i = (int) (movesList.size() * Math.random());
        return movesList.get(i);
    }

    protected boolean isAttackedByCheaperPiece(CellDTO victimCell, List<ExtendedMove> harmfulMove) {
        for (ExtendedMove playerMove : harmfulMove) {
            if (playerMove.getValueFrom() < victimCell.getValue()) {
                return true;
            }
        }
        return false;
    }

    protected void printMap(Map<CellDTO, List<ExtendedMove>> map, String cellName) {
        map.forEach((cell, extendedMoves) -> {
            log.info(cellName + ": " + cell);
            printMoves(extendedMoves);
        });
    }

    protected void printMoves(List<ExtendedMove> moves) {
        moves.forEach(move -> log.info("\t" + CommonUtils.moveToString(move)));
    }

    @Autowired
    public void setGameService(GameService gameService) {
        this.gameService = gameService;
    }

    @Value("${app.game.bot.move-delay}")
    public void setBotMoveDelay(Long botMoveDelay) {
        this.botMoveDelay = botMoveDelay;
    }
}