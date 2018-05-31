package com.example.chess.service;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.Side;

import java.util.List;
import java.util.Set;

public interface MoveService {

	Set<PointDTO> getAvailableMoves(PointDTO point);

	boolean isEnemyKingUnderAttack(Side attackingSide);

	void setGame(Game game);

	void setCellsMatrix(List<List<CellDTO>> cellsMatrix);
}
