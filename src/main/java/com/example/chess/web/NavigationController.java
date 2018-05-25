package com.example.chess.web;

import com.example.chess.exceptions.NavigationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class NavigationController {

	private static final String VIEW = "index";

	@RequestMapping({
			"/",
			"/game/{gameId}",
			"/game/{gameId}/position/{position}"
	})
	public String redirect(@PathVariable(value = "gameId", required = false) String gameId,
						   @PathVariable(value = "position", required = false) String position) throws NavigationException {
		if (gameId != null && !StringUtils.isNumeric(gameId)) {
			throw new NavigationException("<gameId> must be a number");
		}
		if (position != null && !StringUtils.isNumeric(position)) {
			throw new NavigationException("<position> must be a number");
		}
		return VIEW;
	}
}
