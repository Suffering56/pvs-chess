package com.example.chess.web;

import com.example.chess.App;
import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.GameMode;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;
    private final BotService botService;

    @Autowired
    public GameController(GameService gameService, @Qualifier(App.DEFAULT_BOT_MODE) BotService botService) {
        this.gameService = gameService;
        this.botService = botService;
    }

    @GetMapping("/{gameId}/move")
    public Set<PointDTO> getAvailableMoves(@PathVariable("gameId") long gameId,
                                           @RequestParam int rowIndex,
                                           @RequestParam int columnIndex) throws GameNotFoundException {

        return gameService.getAvailableMoves(gameId, PointDTO.valueOf(rowIndex, columnIndex));
    }

    @PostMapping("/{gameId}/move")
    public ArrangementDTO applyMove(@PathVariable("gameId") long gameId,
                                    @RequestBody MoveDTO move) throws GameNotFoundException {

        Game game = gameService.findAndCheckGame(gameId);
        Pair<CellsMatrix, ArrangementDTO> pair = gameService.applyMove(game, move);

        if (game.getMode() == GameMode.AI) {
            botService.applyBotMove(game, move.toExtendedMove(pair.getKey()));
        }
        return pair.getValue();
    }

    @GetMapping("/{gameId}/listen")
    public ArrangementDTO getLastGameArrangement(@PathVariable("gameId") long gameId) throws GameNotFoundException {

        Game game = gameService.findAndCheckGame(gameId);
        return gameService.createArrangementByGame(game, game.getPosition());
    }
}
