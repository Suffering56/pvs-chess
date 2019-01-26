package com.example.chess.exceptions;

import com.example.chess.logic.objects.game.GameContext;
import lombok.Getter;

@Getter
public class CheckmateException extends RuntimeException {

    private GameContext context;

    public CheckmateException(GameContext context) {
        this.context = context;
    }
}
