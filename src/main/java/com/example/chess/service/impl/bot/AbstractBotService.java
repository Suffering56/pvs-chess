package com.example.chess.service.impl.bot;

import com.example.chess.logic.ChessConstants;
import com.example.chess.logic.debug.Debug;
import com.example.chess.aspects.Profile;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.RatingParam;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.Rating;
import com.example.chess.logic.objects.move.ExtendedMove;
import com.example.chess.logic.objects.game.FakeGame;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import com.example.chess.logic.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "SameParameterValue"})
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

    @Profile
    @Override
    public void applyBotMove(Game game) {
        executorService.execute(() -> {
            try {
                CellsMatrix actualMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());
                Side botSide = game.getActiveSide();
                MoveDTO moveDTO = findBestMove(game.toFakeBuilder().build(), actualMatrix, botSide);
                gameService.applyMove(game, moveDTO);
                TimeUnit.SECONDS.sleep(1);
            } catch (GameNotFoundException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    @NoArgsConstructor
    @AllArgsConstructor
    class PositionNode {
        int position;
        CellsMatrix matrix;
        ExtendedMove lastMove;
    }

    private MoveDTO findBestMove(FakeGame fakeGame, CellsMatrix originalMatrix, Side botSide) {
        long start = System.currentTimeMillis();
        Debug.resetCounters();


        MoveHelper.valueOf(fakeGame, originalMatrix)
                .getStandardMovesStream(botSide)
                .peek(potentialMove -> {
                    Piece pieceFromPawn = Piece.of(botSide, PieceType.QUEEN);
                    if (potentialMove.isPawnTransformation()) ;
                    //TODO: piece from pawn
//                    originalMatrix.executeMove(potentialMove, )
                });


        ExtendedMove resultMove = findBestExtendedMove(fakeGame, originalMatrix, botSide, true);
        System.out.println("resultMove[pos=" + originalMatrix.getPosition() + "] = " + resultMove);

        if (resultMove.getTotal() >= ChessConstants.CHECKMATE_VALUE) {
            System.out.println("Bot want checkmate you!!!");
        }

        System.out.println();
        Debug.printCounters();
        System.out.println("findBestMove executed in : " + (System.currentTimeMillis() - start) + "ms");

        PieceType pieceFromPawn = null;
        if (resultMove.getPieceFrom() == PieceType.PAWN && (resultMove.getTo().getRowIndex() == 0 || resultMove.getTo().getRowIndex() == 7)) {
            pieceFromPawn = PieceType.QUEEN;
        }

        MoveDTO predestinedMove = Debug.getPredestinedMove(originalMatrix.getPosition());
        if (predestinedMove != null) {
            return predestinedMove;
        }

        return MoveDTO.valueOf(resultMove.getPointFrom(), resultMove.getPointTo(), pieceFromPawn);
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
                    CellsMatrix firstMatrixPlayerNext = originalMatrix.executeMove(analyzedMove).getNewMatrix();


                    List<ExtendedMove> playerMoves = MoveHelper.valueOf(fakeGame, firstMatrixPlayerNext)
                            .getStandardMovesStream(playerSide)
                            .collect(Collectors.toList());


                    Pair<Integer, Integer> maxPair = playerMoves
                            .stream()
                            .peek(calculateRating(fakeGame, firstMatrixPlayerNext, playerSide, false))
                            .map(playerMove -> {
                                CellsMatrix secondMatrixBotNext = firstMatrixPlayerNext.executeMove(playerMove).getNewMatrix();

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
