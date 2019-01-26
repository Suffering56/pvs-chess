package com.example.chess.service.impl.bot;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.RatingParam;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.CheckmateException;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.logic.ChessConstants;
import com.example.chess.logic.MoveHelper;
import com.example.chess.logic.debug.Debug;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.Rating;
import com.example.chess.logic.objects.game.FakeGame;
import com.example.chess.logic.objects.game.GameContext;
import com.example.chess.logic.objects.game.RootGameContext;
import com.example.chess.logic.objects.move.ExtendedMove;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "SameParameterValue", "Duplicates"})
@Log4j2
public abstract class AbstractBotService implements BotService {

    protected GameService gameService;
    protected Long botMoveDelay;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected enum LoggerParam {
        COMMON, PRINT_SORTED_BOT_MOVES_LIST, PRINT_RESULT_MOVE, MATERIAL;
    }

    private Map<LoggerParam, Boolean> loggerSettings = new EnumMap<LoggerParam, Boolean>(LoggerParam.class) {{
        put(LoggerParam.COMMON, false);
        put(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST, false);
        put(LoggerParam.PRINT_RESULT_MOVE, false);
        put(LoggerParam.MATERIAL, false);
    }};

    protected abstract Consumer<? super ExtendedMove> calculateRating(FakeGame fakeGame, CellsMatrix originalMatrix, Side botSide, boolean isExternalCall);

    protected void calculateRating(GameContext gameContext) {
        throw new UnsupportedOperationException();
    }

    @Profile
    @Override
    public void applyBotMove(Game game, @Nullable ExtendedMove playerLastMove) {
        executorService.execute(() -> {
            try {
                Side botSide = game.getActiveSide();
                CellsMatrix originalMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());

                RootGameContext rootContext = RootGameContext.of(game, originalMatrix, playerLastMove, botSide);

                MoveDTO nextBotMove = findBestMove(rootContext);
                gameService.applyMove(game, nextBotMove);

                TimeUnit.SECONDS.sleep(1);
            } catch (GameNotFoundException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    private void calculateRatingRecursive(GameContext context) throws CheckmateException {
        if (!context.isRoot()) {
            calculateRating(context);
        }
        if (context.hasChildren() && context.getDeep() <= ChessConstants.BOT_ANALYSIS_DEEP) {
            context.childrenStream()
                    .forEach(this::calculateRatingRecursive);
        }
    }

    private MoveDTO findBestMove(RootGameContext rootContext) {
        long start = System.currentTimeMillis();
        Debug.resetCounters();

        rootContext.fill(1);
        if (!rootContext.hasChildren()) {
            throw new RuntimeException("Checkmate: Player win!");
        }
        log.info("rootContext.getTotalMovesCount(): " + rootContext.getTotalMovesCount());

        ExtendedMove resultMove;
        try {
            calculateRatingRecursive(rootContext);
            resultMove = findBestExtendedMove(rootContext);
        } catch (CheckmateException e) {
            resultMove = e.getContext().getLastMove();
        }


        System.out.println("resultMove[pos=" + rootContext.getMatrix().getPosition() + "] = " + resultMove);
        if (resultMove.getTotal() >= ChessConstants.CHECKMATE_VALUE) {
            System.out.println("Bot want checkmate you!!!");
        }
        System.out.println();
        Debug.printCounters();
        System.out.println("findBestMove executed in : " + (System.currentTimeMillis() - start) + "ms");



        MoveDTO predestinedMove = Debug.getPredestinedMove(rootContext.getMatrix().getPosition());
        if (predestinedMove != null) {
            return predestinedMove;
        }

        PieceType pieceFromPawn = resultMove.isPawnTransformation() ? resultMove.getPieceFromPawn() : null;
        return MoveDTO.valueOf(resultMove.getPointFrom(), resultMove.getPointTo(), pieceFromPawn);
    }


    protected ExtendedMove findBestExtendedMove(RootGameContext rootGameContext) {
        List<ExtendedMove> botAvailableMoves = rootGameContext.childrenStream()
                .map(GameContext::getLastMove)
                .sorted(Comparator.comparing(ExtendedMove::getTotal))
                .collect(Collectors.toList());

        if (botAvailableMoves.isEmpty()) {
            return null;
        }

        int max = botAvailableMoves
                .stream()
                .mapToInt(ExtendedMove::getTotal)
                .max().orElseThrow(UnsupportedOperationException::new);

        List<ExtendedMove> topMovesList = botAvailableMoves
                .stream()
                .filter(move -> move.getTotal() == max)
                .collect(Collectors.toList());

        ExtendedMove bestMove = getRandomMove(topMovesList);

        enter(LoggerParam.COMMON, 50);
//        movesWithRating.forEach(move -> log(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST, move));
        logSingleSeparator(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST);

        enter(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST);
        log(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST, "ResultMove[original_pos = " + rootGameContext.getMatrix().getPosition() + "]::::" + bestMove);
        logDoubleSeparator(LoggerParam.COMMON, 3);

        return bestMove;
    }

    protected ExtendedMove findBestExtendedMove(FakeGame fakeGame, CellsMatrix originalMatrix, Side botSide, boolean isExternalCall) {
        List<ExtendedMove> botAvailableMoves = MoveHelper.valueOf(fakeGame, originalMatrix)
                .getStandardMovesStream(botSide)
                .sorted(Comparator.comparing(ExtendedMove::getTotal))
                .collect(Collectors.toList());

        if (botAvailableMoves.isEmpty()) {
            return null;
        }

        botAvailableMoves = executeFirstVariation(fakeGame, originalMatrix, botSide, botAvailableMoves, isExternalCall);
//        botAvailableMoves = executeSecondVariation(fakeGame, originalMatrix, botSide, botAvailableMoves);


        int max = botAvailableMoves
                .stream()
                .mapToInt(ExtendedMove::getTotal)
                .max().orElseThrow(UnsupportedOperationException::new);

        List<ExtendedMove> topMovesList = botAvailableMoves
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

    private List<ExtendedMove> executeFirstVariation(FakeGame fakeGame, CellsMatrix originalMatrix, Side botSide, List<ExtendedMove> botAvailableMoves, boolean isExternalCall) {
        botAvailableMoves.forEach(calculateRating(fakeGame, originalMatrix, botSide, isExternalCall));
        return botAvailableMoves;
    }

    private List<ExtendedMove> executeSecondVariation(FakeGame fakeGame, CellsMatrix originalMatrix, Side botSide, List<ExtendedMove> botAvailableMoves) {
        Side playerSide = botSide.reverse();

        return botAvailableMoves
                .stream()
                .peek(calculateRating(fakeGame, originalMatrix, botSide, false))
                .peek(analyzedMove -> {
                    CellsMatrix firstMatrixPlayerNext = originalMatrix.executeMove(analyzedMove);


                    List<ExtendedMove> playerMoves = MoveHelper.valueOf(fakeGame, firstMatrixPlayerNext)
                            .getStandardMovesStream(playerSide)
                            .collect(Collectors.toList());


                    Pair<Integer, Integer> maxPair = playerMoves
                            .stream()
                            .peek(calculateRating(fakeGame, firstMatrixPlayerNext, playerSide, false))
                            .map(playerMove -> {
                                CellsMatrix secondMatrixBotNext = firstMatrixPlayerNext.executeMove(playerMove);

                                int maxByBot = MoveHelper.valueOf(fakeGame, secondMatrixBotNext)
                                        .getStandardMovesStream(botSide)
                                        .peek(calculateRating(fakeGame, secondMatrixBotNext, botSide, false))
                                        .mapToInt(ExtendedMove::getTotal)
                                        .max().orElse(-ChessConstants.CHECKMATE_VALUE);

//                                return -playerMove.getTotal() + maxByBot;

                                return Pair.of(playerMove.getTotal(), maxByBot);
                            })
                            .reduce((p1, p2) -> {
                                int max1 = -p1.getFirst() + p1.getSecond();
                                int max2 = -p2.getFirst() + p2.getSecond();

                                return max1 >= max2 ? p1 : p2;
                            })
                            .orElse(null);

                    if (maxPair == null) {
                        //checkmate by bot
                        analyzedMove.updateRating(Rating.builder().build(RatingParam.CHECKMATE_BY_BOT));
                        return;
                    }

                    analyzedMove.updateRating(Rating.builder().build(RatingParam.DEEP_2_BY_PLAYER, maxPair.getFirst()));
                    analyzedMove.updateRating(Rating.builder().build(RatingParam.DEEP_3_BY_BOT, maxPair.getSecond()));
                })
                .collect(Collectors.toList());
    }

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
