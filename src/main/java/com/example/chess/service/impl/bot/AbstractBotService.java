package com.example.chess.service.impl.bot;

import com.example.chess.App;
import com.example.chess.aspects.Profile;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
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

@SuppressWarnings({"WeakerAccess", "SameParameterValue"})
@Log4j2
public abstract class AbstractBotService implements BotService {

    protected GameService gameService;
    protected Long botMoveDelay;


    protected enum LoggerParam {
        COMMON, PRINT_SORTED_BOT_MOVES_LIST, PRINT_RESULT_MOVE, MATERIAL;
    }

    private Map<LoggerParam, Boolean> loggerSettings = new HashMap<LoggerParam, Boolean>() {{
        put(LoggerParam.COMMON, false);
        put(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST, false);
        put(LoggerParam.PRINT_RESULT_MOVE, false);
        put(LoggerParam.MATERIAL, false);
    }};

    private Map<Integer, MoveDTO> destinyMap = new HashMap<Integer, MoveDTO>() {{
        // "e7---e6"
        put(1, MoveDTO.valueOf(PointDTO.valueOf(6, 3), PointDTO.valueOf(5, 3), null));
        // "Bf8---b4"
        put(3, MoveDTO.valueOf(PointDTO.valueOf(7, 2), PointDTO.valueOf(3, 6), null));
    }};

    @Profile
    @Override
    public void applyBotMove(Game game) {
        CommonUtils.executeInSecondaryThread(() -> {
            CellsMatrix cellsMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());
            Side readyToMoveSide = game.getReadyToMoveSide();
            MoveDTO moveDTO = findBestMove(game.toFake(), cellsMatrix, readyToMoveSide);
            gameService.applyMove(game, moveDTO);
        });
    }

    protected abstract Consumer<? super ExtendedMove> calculateRating(FakeGame game, CellsMatrix originalMatrix, Side readyToMoveSide, boolean isExternalCall);

    protected MoveDTO findBestMove(FakeGame fakeGame, CellsMatrix originalMatrix, Side readyToMoveSide) {
        long start = System.currentTimeMillis();
        App.resetCounters();


        ExtendedMove resultMove = findBestExtendedMove(fakeGame, originalMatrix, readyToMoveSide, true);

        System.out.println();
        App.printCounters();
        System.out.println("findBestMove executed in : " + (System.currentTimeMillis() - start) + "ms");

        PieceType promotionPieceType = null;
        if (resultMove.getPieceFrom() == PieceType.PAWN && (resultMove.getTo().getRowIndex() == 0 || resultMove.getTo().getRowIndex() == 7)) {
            promotionPieceType = PieceType.QUEEN;
        }

        MoveDTO predestinedMove = destinyMap.get(originalMatrix.getPosition());
        if (predestinedMove != null) {
            return predestinedMove;
        }

        return MoveDTO.valueOf(resultMove.getPointFrom(), resultMove.getPointTo(), promotionPieceType);
    }

    protected ExtendedMove findBestExtendedMove(FakeGame fakeGame, CellsMatrix originalMatrix, Side readyToMoveSide, boolean isExternalCall) {
        List<ExtendedMove> sortedBotMovesList = MoveHelper.valueOf(fakeGame, originalMatrix)
                .getStandardMovesStream(readyToMoveSide)
                .peek(calculateRating(fakeGame, originalMatrix, readyToMoveSide, isExternalCall))
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

        enter(LoggerParam.COMMON, 50);
        sortedBotMovesList.forEach(move -> log(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST, move));
        logSingleSeparator(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST);

        enter(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST);
        log(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST, "ResultMove[original_pos = " + originalMatrix.getPosition() + "]::::" + resultMove);
        logDoubleSeparator(LoggerParam.COMMON, 3);

        return resultMove;
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

    @Autowired
    public void setGameService(GameService gameService) {
        this.gameService = gameService;
    }

    @Value("${app.game.bot.move-delay}")
    public void setBotMoveDelay(Long botMoveDelay) {
        this.botMoveDelay = botMoveDelay;
    }

    protected void log(LoggerParam loggerParam, Object message, Object... params) {
        if (loggerSettings.get(loggerParam)) {
            log.info(message.toString(), params);
        }
    }


    protected void enter(LoggerParam loggerParam, int count) {
        if (loggerSettings.get(loggerParam)) {
            for (int i = 0; i < count; i++) {
                System.out.println();
            }
        }
    }

    protected void enter(LoggerParam loggerParam) {
        enter(loggerParam, 1);
    }

    protected void logDoubleSeparator(LoggerParam loggerParam, int count) {
        for (int i = 0; i < count; i++) {
            log(loggerParam, "====================================");
        }
    }

    protected void logSingleSeparator(LoggerParam loggerParam, int count) {
        for (int i = 0; i < count; i++) {
            log(loggerParam, "----------------------------------");
        }
    }

    protected void logSingleSeparator(LoggerParam loggerParam) {
        logSingleSeparator(loggerParam, 1);
    }
}
