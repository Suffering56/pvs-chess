package com.example.chess.web;

import com.example.chess.App;
import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.ModeDTO;
import com.example.chess.dto.SideDTO;
import com.example.chess.entity.Game;
import com.example.chess.entity.GameFeatures;
import com.example.chess.enums.GameMode;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.repository.GameRepository;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import com.example.chess.logic.utils.CustomResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;

@RestController
@RequestMapping("/api/init")
public class InitController {

    private final GameService gameService;
    private final GameRepository gameRepository;
    private final BotService botService;

    @Autowired
    public InitController(GameService gameService, GameRepository gameRepository,
                          @Qualifier(App.DEFAULT_BOT_MODE) BotService botService) {
        this.gameService = gameService;
        this.gameRepository = gameRepository;
        this.botService = botService;
    }

    @GetMapping
    public Game createGame() {
        Game game = new Game();
        game.setFeaturesMap(new HashMap<Side, GameFeatures>() {{
            put(Side.WHITE, new GameFeatures(game, Side.WHITE));
            put(Side.BLACK, new GameFeatures(game, Side.BLACK));
        }});

        return gameRepository.save(game);
    }

    @GetMapping("/{gameId}")
    public Game getGame(@PathVariable("gameId") long gameId) throws GameNotFoundException {
        return gameService.findAndCheckGame(gameId);
    }

    @PostMapping("/{gameId}/mode")
    @ResponseStatus(value = HttpStatus.OK)
    public CustomResponse setMode(@PathVariable("gameId") Long gameId,
                                  @RequestBody ModeDTO dto) throws GameNotFoundException {

        Game game = gameService.findAndCheckGame(gameId);
        game.setMode(dto.getMode());
        gameRepository.save(game);

        return CustomResponse.createVoid();
    }

    @GetMapping("/{gameId}/side")
    public SideDTO getSideBySessionId(@PathVariable("gameId") Long gameId,
                                      HttpServletRequest request) throws Exception {

        Game game = gameService.findAndCheckGame(gameId);
        String sessionId = request.getSession().getId();

        int freeSlotsCount = 0;
        Side freeSide = null;

        for (GameFeatures features : game.getFeaturesMap().values()) {
            if (sessionId.equals(features.getSessionId())) {
                return new SideDTO(features.getSide());
            }

            if (features.getSessionId() == null) {
                freeSlotsCount++;
                freeSide = features.getSide();
            }
        }

        if (freeSlotsCount == 2) {
            //unselected
            return SideDTO.createUnselected();
        }

        //TODO: здесь нужно добавить проверку на то как давно пользователь был неактивен.

        if (freeSlotsCount == 0) {
            //no free slots => viewer mode
            return new SideDTO(SideDTO.VIEWER);
        } else {    //freeSlotsCount = 1
            //take free slot
            return new SideDTO(freeSide);
        }
    }

    @PostMapping("/{gameId}/side")
    @ResponseStatus(value = HttpStatus.OK)
    public CustomResponse setSide(@PathVariable("gameId") Long gameId,
                                  @RequestBody SideDTO dto,
                                  HttpServletRequest request) throws GameNotFoundException {

        if (SideDTO.VIEWER.equals(dto.getSide())) {
            return CustomResponse.createVoid();
        }

        Game game = gameService.findAndCheckGame(gameId);
        String sessionId = request.getSession().getId();

        Side side = dto.getSideAsEnum();
        game.getSideFeatures(side).setSessionId(sessionId);
        game.getSideFeatures(side).setLastVisitDate(LocalDateTime.now());

        gameRepository.save(game);

        return CustomResponse.createVoid();
    }

    @GetMapping("/{gameId}/arrangement/{position}")
    public ArrangementDTO getArrangementByPosition(@PathVariable("gameId") long gameId,
                                                   @PathVariable("position") int position) throws Exception {

        Game game = gameService.findAndCheckGame(gameId);
        ArrangementDTO result = gameService.createArrangementByGame(game, position);

        if (game.getMode() == GameMode.AI && game.getPlayerSide() == Side.BLACK) {
            botService.applyBotMove(game);
        }
        return result;
    }


}
