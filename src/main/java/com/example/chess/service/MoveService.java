package com.example.chess.service;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.output.CellDTO;

import java.util.List;

public interface MoveService {

	List<PointDTO> getAvailableMoves(List<List<CellDTO>> cellsMatrix, PointDTO point);
}
