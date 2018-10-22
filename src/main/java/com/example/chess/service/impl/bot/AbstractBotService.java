package com.example.chess.service.impl.bot;

import com.example.chess.ChessConstants;
import com.example.chess.Debug;
import com.example.chess.aspects.Profile;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.UnattainablePointException;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import com.example.chess.service.support.CellsMatrix;
import com.example.chess.service.support.ExtendedMove;
import com.example.chess.service.support.FakeGame;
import com.example.chess.service.support.MoveHelper;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Profile
    @Override
    public void applyBotMove(Game game) {
        CommonUtils.executeInSecondaryThread(() -> {
            CellsMatrix cellsMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());
            Side botSide = game.getReadyToMoveSide();
            MoveDTO moveDTO = findBestMove(game.toFake(), cellsMatrix, botSide);
            gameService.applyMove(game, moveDTO);
        });
    }

    protected abstract Consumer<? super ExtendedMove> calculateRating(FakeGame fakeGame, CellsMatrix originalMatrix, List<ExtendedMove> botMovesByOriginal, Side botSide, boolean isExternalCall);

    protected MoveDTO findBestMove(FakeGame fakeGame, CellsMatrix originalMatrix, Side botSide) {
        long start = System.currentTimeMillis();
        Debug.resetCounters();


        ExtendedMove resultMove = findBestExtendedMove(fakeGame, originalMatrix, botSide, true);
        System.out.println("resultMove = " + resultMove);

        if (resultMove.getTotal() >= ChessConstants.CHECKMATE_VALUE) {
            System.out.println("Bot want checkmate you!!!");
        }

        System.out.println();
        Debug.printCounters();
        System.out.println("findBestMove executed in : " + (System.currentTimeMillis() - start) + "ms");

        PieceType promotionPieceType = null;
        if (resultMove.getPieceFrom() == PieceType.PAWN && (resultMove.getTo().getRowIndex() == 0 || resultMove.getTo().getRowIndex() == 7)) {
            promotionPieceType = PieceType.QUEEN;
        }

        MoveDTO predestinedMove = Debug.getPredestinedMove(originalMatrix.getPosition());
        if (predestinedMove != null) {
            return predestinedMove;
        }

        return MoveDTO.valueOf(resultMove.getPointFrom(), resultMove.getPointTo(), promotionPieceType);
    }

    protected ExtendedMove findBestExtendedMove(FakeGame fakeGame, CellsMatrix originalMatrix, Side botSide, boolean isExternalCall) {
        Side playerSide = botSide.reverse();

        List<ExtendedMove> botAvailableMovesDeep1 = MoveHelper.valueOf(fakeGame, originalMatrix)
                .getStandardMovesStream(botSide)
                .sorted(Comparator.comparing(ExtendedMove::getTotal))
                .collect(Collectors.toList());

        if (botAvailableMovesDeep1.isEmpty()) {
            return null;
        }

        botAvailableMovesDeep1.forEach(calculateRating(fakeGame, originalMatrix, botAvailableMovesDeep1, botSide, isExternalCall));

        int max = botAvailableMovesDeep1
                .stream()
                .mapToInt(ExtendedMove::getTotal)
                .max().orElseThrow(UnattainablePointException::new);

        List<ExtendedMove> topMovesList = botAvailableMovesDeep1
                .stream()
                .filter(move -> move.getTotal() == max)
                .collect(Collectors.toList());

        ExtendedMove bestMove = getRandomMove(topMovesList);

        enter(LoggerParam.COMMON, 50);
//        movesWithRating.forEach(move -> log(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST, move));
        logSingleSeparator(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST);

        enter(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST);
        log(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST, "ResultMove[original_pos = " + originalMatrix.getPosition() + "]::::" + bestMove);
        logDoubleSeparator(LoggerParam.COMMON, 3);

        return bestMove;
    }


//
//        botAvailableMovesDeep1
//                .parallelStream()
//                .peek(calculateRating(fakeGame, originalMatrix, botAvailableMovesDeep1, botSide, false))
//                .map(analyzedMove -> {
//                    CellsMatrix matrixDeep1 = originalMatrix.executeMove(analyzedMove.toMoveDTO(), null).getNewMatrix();
//
//                    List<ExtendedMove> playerAvailableMovesDeep2 = MoveHelper.valueOf(fakeGame, matrixDeep1)
//                            .getStandardMovesStream(playerSide)
//                            .collect(Collectors.toList());
//
//                    List<ExtendedMove> playerMovesWithRatingDeep2 = playerAvailableMovesDeep2
//                            .stream()
//                            .peek(calculateRating(fakeGame, originalMatrix, playerAvailableMovesDeep2, playerSide, false))
//                            .collect(Collectors.toList());
//
//                    int playerMaxByDeep2 = playerMovesWithRatingDeep2
//                            .stream()
//                            .mapToInt(ExtendedMove::getTotal)
//                            .max()
//                            .orElse(ChessConstants.CHECKMATE_VALUE);
//
//                    int botMaxByDeep3 = playerMovesWithRatingDeep2
//                            .stream()
//                            .map(playerMove -> {
//                                CellsMatrix matrixDeep2 = matrixDeep1.executeMove(playerMove.toMoveDTO(), null).getNewMatrix();
//
//                                List<ExtendedMove> botAvailableMovesDeep3 = MoveHelper.valueOf(fakeGame, matrixDeep2)
//                                        .getStandardMovesStream(playerSide)
//                                        .collect(Collectors.toList());
//
//                                if (botAvailableMovesDeep3.isEmpty()) {
//                                    throw new RuntimeException("Checkmate by player deep 3!!!");
//                                }
//
//                                OptionalInt positiveMaxDeep3 = botAvailableMovesDeep3
//                                        .stream()
//                                        .peek(calculateRating(fakeGame, matrixDeep2, botAvailableMovesDeep3, botSide, false))
//                                        .mapToInt(ExtendedMove::getTotal)
//                                        .max();
//
//                                return positiveMaxDeep3.orElse(-ChessConstants.CHECKMATE_VALUE);
//                            })
//                            .mapToInt(Integer::intValue)
//                            .max()
//                            .orElse(ChessConstants.CHECKMATE_VALUE);
//
//
//                })

    //                .map(botMove -> {
//                    MoveData moveData = originalMatrix.executeMove(botMove, 1);
//                    fillDeeperMoves(fakeGame, moveData, 4);
//                    return moveData;
//                });
//                .collect(Collectors.toList());



    protected ExtendedMove getRandomMove(List<ExtendedMove> movesList) {
        int i = (int) (movesList.size() * Math.random());
        return movesList.get(i);
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
