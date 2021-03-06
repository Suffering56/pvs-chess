package com.example.chess.web;

import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;
import com.example.chess.entity.History;
import com.example.chess.enums.GameMode;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.ExtendedMove;
import com.example.chess.repository.GameRepository;
import com.example.chess.repository.HistoryRepository;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author v.peschaniy
 *      Date: 19.02.2019
 */

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final GameService gameService;
    private final BotService botService;
    private final GameRepository gameRepository;
    private final HistoryRepository historyRepository;

    @Autowired
    public DebugController(GameService gameService, BotService botService, GameRepository gameRepository, HistoryRepository historyRepository) {
        this.gameService = gameService;
        this.botService = botService;
        this.gameRepository = gameRepository;
        this.historyRepository = historyRepository;
    }

    @GetMapping("/{gameId}/reset")
    public String[] resetGame(@PathVariable("gameId") long gameId) throws GameNotFoundException {
        Game game = gameService.findAndCheckGame(gameId);
        game.reset();

        historyRepository.deleteAll(findAllGameHistory(gameId));
        gameRepository.save(game);

        return getHistory(gameId);
    }

    @GetMapping("/{gameId}/move/{moveStr}")
    public String[] applyMove(@PathVariable("gameId") long gameId,
                              @PathVariable String moveStr) throws GameNotFoundException {

        Game game = gameService.findAndCheckGame(gameId);
        CellsMatrix originalMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());

        MoveDTO move = MoveDTO.valueOf(moveStr);
        CellDTO cellFrom = originalMatrix.getCell(move.getFrom());

        Preconditions.checkArgument(!cellFrom.isEmpty(), "incorrect pointFrom: cell is empty");
        Preconditions.checkArgument(cellFrom.getSide() == Side.getNextTurnSideByPosition(game.getPosition()), "incorrect pointFrom: is not your turn");

        gameService.applyMove(game, move);
        return getHistory(gameId);
    }

    @GetMapping("/{gameId}/history")
    public String[] getHistory(@PathVariable("gameId") long gameId) throws GameNotFoundException {
        gameService.findAndCheckGame(gameId);

        return findAllGameHistory(gameId)
                .stream()
                .map(move -> String.format("move[%s]: %s (%s)", move.getFormattedPosition(), move.toReadableString(), Side.getNextTurnSideByPosition(move.getPosition()).reverse()))
                .toArray(String[]::new);
    }


    //TODO: move to service
    private List<History> findAllGameHistory(long gameId) {
        return historyRepository.findByGameIdAndPositionLessThanEqualOrderByPositionAsc(gameId, Integer.MAX_VALUE);
    }

    @GetMapping("/{gameId}/rollback")
    public ArrangementDTO rollbackLastMove(@PathVariable("gameId") long gameId) throws GameNotFoundException {

        Game game = gameService.findAndCheckGame(gameId);
        return gameService.rollbackLastMove(game);
    }

    @GetMapping("/{gameId}/wake")
    public void wakeBot(@PathVariable("gameId") long gameId) throws GameNotFoundException {
        Game game = gameService.findAndCheckGame(gameId);

        Preconditions.checkState(game.getMode() == GameMode.AI, "You should to use AI_MODE!");
        Preconditions.checkState(game.getPlayerSide() != game.getActiveSide(), "Is player turn!");

        History lastMove = gameService.findLastMove(game);
        CellsMatrix matrix = gameService.createCellsMatrixByGame(game, game.getPosition() - 1);

        game.setUnderCheckSide(null);
        game = gameRepository.save(game);

        ExtendedMove lastExtMove = null;
        if (lastMove != null) {
            lastExtMove = lastMove.toExtendedMove(matrix);
        }

        botService.applyBotMove(game, lastExtMove);
    }

    @GetMapping("/{gameId}/rollbackAndWake")
    public ImmutableMap<String, String[]> rollbackAndWake(@PathVariable("gameId") long gameId) throws GameNotFoundException, InterruptedException {
        String[] oldHistory = getHistory(gameId);
        int expectedMovesCount = getHistory(gameId).length;
        rollbackLastMove(gameId);
        wakeBot(gameId);

        String[] newHistory = getHistory(gameId);
        while (newHistory.length < expectedMovesCount) {
            Thread.sleep(500);
            newHistory = getHistory(gameId);
        }

        return ImmutableMap.of("old", oldHistory, "new", newHistory);
    }
}
