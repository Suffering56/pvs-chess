package com.example.chess.service;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.CellDTO;
import com.example.chess.entity.Game;

import java.util.List;
import java.util.Set;

public interface MoveService {

	Set<PointDTO> getAvailableMoves(Game game, List<List<CellDTO>> cellsMatrix, PointDTO point);
}
