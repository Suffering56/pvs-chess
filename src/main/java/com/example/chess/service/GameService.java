package com.example.chess.service;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.ArrangementDTO;
import com.example.chess.entity.Game;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.exceptions.HistoryNotFoundException;

import java.util.Set;

public interface GameService {

	ArrangementDTO getArrangementByPosition(long gameId, int position) throws HistoryNotFoundException;

	Set<PointDTO> getAvailableMoves(long gameId, PointDTO selectedCell) throws GameNotFoundException, HistoryNotFoundException;

	ArrangementDTO applyMove(long gameId, MoveDTO dto) throws HistoryNotFoundException, GameNotFoundException;

	Game findAndCheckGame(long gameId) throws GameNotFoundException;
}
