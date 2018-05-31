package com.example.chess.web;

import com.example.chess.dto.SideChooseDTO;
import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.ParamsPlayerDTO;
import com.example.chess.entity.Game;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.repository.GameRepository;
import com.example.chess.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@RestController
@RequestMapping("/api/init")
public class InitController {

	private final GameService gameService;
	private final GameRepository gameRepository;

	@Autowired
	public InitController(GameService gameService, GameRepository gameRepository) {
		this.gameService = gameService;
		this.gameRepository = gameRepository;
	}

	@GetMapping
	public Game createGame() {
		return gameRepository.save(new Game());
	}

	@GetMapping("/{gameId}")
	public Game getGame(@PathVariable("gameId") long gameId) throws GameNotFoundException {
		return gameService.findAndCheckGame(gameId);
	}

	@GetMapping("/{gameId}/side")
	public ParamsPlayerDTO getSide(@PathVariable("gameId") Long gameId,
								   HttpServletRequest request) throws Exception {

		Game game = gameService.findAndCheckGame(gameId);
		String sessionId = request.getSession().getId();

		if (game.getWhiteSessionId() == null && game.getBlackSessionId() == null) {
			//unselected
			return new ParamsPlayerDTO(null, false);
		}

		if (Objects.equals(game.getWhiteSessionId(), sessionId)) {
			//white (selected)
			return new ParamsPlayerDTO(true, false);
		}

		if (Objects.equals(game.getBlackSessionId(), sessionId)) {
			//black (selected)
			return new ParamsPlayerDTO(false, false);
		}

		if (game.getWhiteSessionId() != null && game.getBlackSessionId() != null) {
			//viewer
			return new ParamsPlayerDTO(null, true);
		}

		if (game.getWhiteSessionId() == null) {
			//white (unselected)
			return new ParamsPlayerDTO(true, false);
		}

		if (game.getBlackSessionId() == null) {
			//black (unselected)
			return new ParamsPlayerDTO(false, false);
		}

		throw new Exception("This should not have happened!");
	}

	@PostMapping("/{gameId}/side")
	@ResponseStatus(value = HttpStatus.OK)
	public void setSide(@PathVariable("gameId") Long gameId,
						@RequestBody SideChooseDTO dto,
						HttpServletRequest request) throws GameNotFoundException {

		Game game = gameService.findAndCheckGame(gameId);
		String sessionId = request.getSession().getId();

		if (dto.getIsWhite()) {
			game.setWhiteSessionId(sessionId);
		} else {
			game.setBlackSessionId(sessionId);
		}

		gameRepository.save(game);
	}

	@GetMapping("/{gameId}/arrangement/{position}")
	public ArrangementDTO getArrangementByPosition(@PathVariable("gameId") long gameId,
												   @PathVariable("position") int position) throws Exception {

		return gameService.getArrangementByPosition(gameId, position);
	}


}
