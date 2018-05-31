package com.example.chess.web;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.ArrangementDTO;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.exceptions.HistoryNotFoundException;
import com.example.chess.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/game")
public class GameController {

	private final GameService gameService;

	@Autowired
	public GameController(GameService gameService) {
		this.gameService = gameService;
	}

	@GetMapping("/{gameId}/move")
	public Set<PointDTO> getAvailableMoves(@PathVariable("gameId") long gameId,
										   @RequestParam int rowIndex,
										   @RequestParam int columnIndex) throws HistoryNotFoundException, GameNotFoundException {

		return gameService.getAvailableMoves(gameId, new PointDTO(rowIndex, columnIndex));
	}

	@PostMapping("/{gameId}/move")
	public ArrangementDTO applyMove(@PathVariable("gameId") long gameId,
									@RequestBody MoveDTO dto) throws GameNotFoundException, HistoryNotFoundException {

		return gameService.applyMove(gameId, dto);
	}
}
