package com.example.chess.service;

import com.example.chess.dto.CellsMatrix;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;

public interface BotService {
    void setGame(Game game);

    void setCellsMatrix(CellsMatrix matrix);

    MoveDTO generateBotMove();
}
