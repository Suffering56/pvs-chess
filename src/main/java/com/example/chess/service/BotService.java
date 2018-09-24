package com.example.chess.service;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;

import java.util.List;

public interface BotService {
    void setGame(Game game);

    void setCellsMatrix(List<List<CellDTO>> cellsMatrix);

    MoveDTO generateBotMove();
}
