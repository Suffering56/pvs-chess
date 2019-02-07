package com.example.chess.service.impl.bot;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.CheckmateException;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.logic.debug.Debug;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.game.GameContext;
import com.example.chess.logic.objects.game.RootGameContext;
import com.example.chess.logic.objects.move.ExtendedMove;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Log4j2
@SuppressWarnings({"WeakerAccess", "SameParameterValue", "Duplicates"})
public abstract class AbstractBotService implements BotService {

    protected GameService gameService;
    protected Long botMoveDelay;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected void calculateRating(GameContext gameContext) {
        throw new UnsupportedOperationException();
    }

    @Autowired
    public void setGameService(GameService gameService) {
        this.gameService = gameService;
    }

    @Value("${app.game.bot.move-delay}")
    public void setBotMoveDelay(Long botMoveDelay) {
        this.botMoveDelay = botMoveDelay;
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

    private MoveDTO findBestMove(RootGameContext rootContext) {
        int deep = 3;
        Debug.resetCounters();
        long start = System.currentTimeMillis();

        rootContext.fill(deep);
        if (!rootContext.hasChildren()) {
            throw new RuntimeException("Checkmate: Player win!");
        }

        log.info("totalMovesCount[before calculation]: " + rootContext.getTotalMovesCount());

        ExtendedMove resultMove;
        try {
            calculateRatingRecursive(rootContext, deep);
            resultMove = findBestExtendedMove(rootContext);
        } catch (CheckmateException e) {
            resultMove = e.getContext().getLastMove();
        }

        log.info("totalMovesCount[after calculation]: " + rootContext.getTotalMovesCount());


        MoveDTO predestinedMove = Debug.getPredestinedMove(rootContext.getMatrix().getPosition());
        if (predestinedMove != null) {
            return predestinedMove;
        }

        PieceType pieceFromPawn = resultMove.isPawnTransformation() ? resultMove.getPieceFromPawn() : null;

        System.out.println();
        Debug.printCounters();
        log.info("ResultMove[original_pos = " + rootContext.getMatrix().getPosition() + "]::::" + resultMove);
        System.out.println();
        System.out.println("findBestMove executed in : " + (System.currentTimeMillis() - start) + "ms");

        return MoveDTO.valueOf(resultMove.getPointFrom(), resultMove.getPointTo(), pieceFromPawn);
    }

    private void calculateRatingRecursive(GameContext context, int deep) throws CheckmateException {
        if (deep < 0) {
            return;
        }

        if (!context.isRoot()) {
            calculateRating(context);
        }
        if (context.hasChildren()) {
            if (context.isRoot()) {
                context.childrenStream()
                        .parallel()
                        .forEach(childContext -> calculateRatingRecursive(childContext, deep - 1));
            } else {
                context.childrenStream()
                        .forEach(childContext -> calculateRatingRecursive(childContext, deep - 1));
            }
        }
    }

    private ExtendedMove findBestExtendedMove(RootGameContext rootGameContext) {
        List<GameContext> rootChildren = rootGameContext.childrenStream()
                //FIXME: надо учитывать тоталы и более глубоких ходов
                .sorted(Comparator.comparing(GameContext::getTotal))
                .collect(Collectors.toList());

        int max = rootChildren
                .stream()
                .mapToInt(GameContext::getTotal)
                .max().orElseThrow(UnsupportedOperationException::new);

        List<GameContext> topMovesList = rootChildren
                .stream()
                .filter(context -> context.getTotal() == max)
                .collect(Collectors.toList());

        return getRandomMove(topMovesList);
    }

    protected ExtendedMove getRandomMove(List<GameContext> movesList) {
        int i = (int) (movesList.size() * Math.random());
        return movesList.get(i).getLastMove();
    }

}
