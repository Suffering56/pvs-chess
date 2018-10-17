package com.example.chess.service.impl.bot;

import com.example.chess.Debug;
import com.example.chess.aspects.Profile;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.KingNotFoundException;
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
import java.util.stream.Stream;

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
        if (resultMove == null) {
            throw new RuntimeException("Checkmate!!!");
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

        List<ExtendedMove> botMovesByOriginal = MoveHelper.valueOf(fakeGame, originalMatrix)
                .getStandardMovesStream(botSide)
                .sorted(Comparator.comparing(ExtendedMove::getTotal))
                .collect(Collectors.toList());

//        Stream<ExtendedMove> botMovesStream;
//        if (Debug.IS_PARALLEL) {
//            botMovesStream = botMovesByOriginal.parallelStream();
//        } else {
//            botMovesStream = botMovesByOriginal.stream();
//        }
//
//        List<MoveData> rootMoveData = botMovesStream
//                .map(botMove -> {
//                    MoveData moveData = originalMatrix.executeMove(botMove, 1);
//                    fillDeeperMoves(fakeGame, moveData, 4);
//                    return moveData;
//                })
//                .collect(Collectors.toList());


        botMovesByOriginal.forEach(calculateRating(fakeGame, originalMatrix, botMovesByOriginal, botSide, isExternalCall));


//        List<MoveData> collect = botMovesByOriginal.stream()
//                .map(botMove -> {
//                    MoveData moveData = originalMatrix.executeMove(botMove, 1);
//                    CellsMatrix firstMatrixPlayerNext = moveData.getNextMatrix();
//
//                    //FIXME: update FakeGame(need new instance)
//                    Map<PointDTO, MoveData> moreDeepPlayerMoves = MoveHelper.valueOf(fakeGame, firstMatrixPlayerNext)
//                            .getStandardMovesStream(playerSide)
//                            .collect(Collectors.toMap(ExtendedMove::getPointTo,
//                                    playerMove -> firstMatrixPlayerNext.executeMove(playerMove, 2)));
//
//                    moveData.setMoreDeepMoves(moreDeepPlayerMoves);
//
//                    moreDeepPlayerMoves
//                            .values()
//                            .forEach(playerMoveData -> {
//                                CellsMatrix secondMatrixBotNext = playerMoveData.getNextMatrix();
//
//                                //FIXME: update FakeGame(need new instance)
//                                Map<PointDTO, MoveData> moreDeepBotMoves = MoveHelper.valueOf(fakeGame, secondMatrixBotNext)
//                                        .getStandardMovesStream(botSide)
//                                        .collect(Collectors.toMap(ExtendedMove::getPointTo,
//                                                deepBotMove -> secondMatrixBotNext.executeMove(deepBotMove, 3)));
//
//                                playerMoveData.setMoreDeepMoves(moreDeepBotMoves);
//                            });
//
//                    return moveData;
//                })
//                .collect(Collectors.toList());

        OptionalInt maxTotal = botMovesByOriginal.stream()
                .mapToInt(ExtendedMove::getTotal)
                .max();

        if (!maxTotal.isPresent()) {    //TODO: checkmate
            return null;
        }

        List<ExtendedMove> topMovesList = botMovesByOriginal.stream()
                .filter(extendedMove -> extendedMove.getTotal() == maxTotal.getAsInt())
                .collect(Collectors.toList());

        ExtendedMove resultMove = getRandomMove(topMovesList);

        enter(LoggerParam.COMMON, 50);
        botMovesByOriginal.forEach(move -> log(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST, move));
        logSingleSeparator(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST);

        enter(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST);
        log(LoggerParam.PRINT_SORTED_BOT_MOVES_LIST, "ResultMove[original_pos = " + originalMatrix.getPosition() + "]::::" + resultMove);
        logDoubleSeparator(LoggerParam.COMMON, 3);

        return resultMove;
    }

    private void fillDeeperMoves(FakeGame fakeGame, MoveData moveData, int maxDeep) {
        CellsMatrix matrixAfterMove = moveData.getNextMatrix();

        Side nextSide = moveData.getExecutedMoveSide().reverse();
        int nextDeep = moveData.getDeep() + 1; //2

        //FIXME: update FakeGame(need new instance)\
        //FIXME: toMap replace by Collectors.groupingBy
//        Map<PointDTO, MoveData> moreDeepMoves = MoveHelper.valueOf(fakeGame, matrixAfterMove)
//                .getStandardMovesStream(nextSide)
//                .collect(Collectors.toMap(ExtendedMove::getPointTo,
//                        deeperMove -> matrixAfterMove.executeMove(deeperMove, nextDeep)));


        Stream<ExtendedMove> movesStream = MoveHelper.valueOf(fakeGame, matrixAfterMove)
                .getStandardMovesStream(nextSide);

        if (Debug.IS_PARALLEL) {
            movesStream = movesStream.parallel();
        }

        List<MoveData> moreDeepMoves = movesStream
                .map(deeperMove -> matrixAfterMove.executeMove(deeperMove, nextDeep))
                .collect(Collectors.toList());

        moveData.setMoreDeepMoves(moreDeepMoves);

        if (nextDeep + 1 > maxDeep) {
            return;
        }

        moreDeepMoves
//                .values()
                .forEach(nextMoveData -> fillDeeperMoves(fakeGame, nextMoveData, maxDeep));
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
