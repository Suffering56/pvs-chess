package com.example.chess.web;

import com.example.chess.dto.ArrangementDTO;
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

    @GetMapping("/{gameId}/history")
    public String[] getHistory(@PathVariable("gameId") long gameId) throws GameNotFoundException {
        Game game = gameService.findAndCheckGame(gameId);
        List<History> history = historyRepository.findByGameIdAndPositionLessThanEqualOrderByPositionAsc(game.getId(), Integer.MAX_VALUE);

        return history.stream()
                .map(move -> String.format("move[%s]: %s (%s)", move.getFormattedPosition(), move.toReadableString(), Side.ofPosition(move.getPosition())))
                .toArray(String[]::new);
    }

    @GetMapping("/{gameId}/rollback")
    public ArrangementDTO rollbackLastMove(@PathVariable("gameId") long gameId) throws GameNotFoundException {

        Game game = gameService.findAndCheckGame(gameId);
        return gameService.rollbackLastMove(game);
    }

    @GetMapping("/{gameId}/wake")
    public void wakeBot(@PathVariable("gameId") long gameId) throws GameNotFoundException {
        Game game = gameService.findAndCheckGame(gameId);
        if (game.getMode() != GameMode.AI) {
            throw new RuntimeException("You should to use AI_MODE!");
        }
        if (game.getPlayerSide() == game.getActiveSide()) {
            throw new RuntimeException("Is player turn!");
        }

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
}
