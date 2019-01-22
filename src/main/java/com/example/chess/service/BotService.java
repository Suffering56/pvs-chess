package com.example.chess.service;

import com.example.chess.entity.Game;
import com.example.chess.logic.objects.move.ExtendedMove;

import javax.annotation.Nullable;

public interface BotService {

    void applyBotMove(Game game, @Nullable ExtendedMove move);
}
