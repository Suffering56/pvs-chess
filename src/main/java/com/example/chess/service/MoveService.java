package com.example.chess.service;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.output.CellDTO;
import com.example.chess.entity.Game;

import java.util.List;

public interface MoveService {

	List<PointDTO> getAvailableMoves(Game game, List<List<CellDTO>> cellsMatrix, PointDTO point);
}
