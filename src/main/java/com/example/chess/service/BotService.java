package com.example.chess.service;

import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;
import com.example.chess.service.support.CellsMatrix;

public interface BotService {

    void applyBotMove(Game game);
}
