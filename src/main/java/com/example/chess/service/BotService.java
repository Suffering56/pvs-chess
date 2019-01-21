package com.example.chess.service;

import com.example.chess.entity.Game;
import com.example.chess.logic.objects.move.ExtendedMove;

public interface BotService {

    void applyBotMove(Game game, ExtendedMove move);
}
