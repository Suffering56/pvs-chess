package com.example.chess.service;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.ArrangementDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.exceptions.HistoryNotFoundException;

import java.util.Set;

public interface GameService {

	ArrangementDTO getArrangementByPosition(Game game, int position) throws HistoryNotFoundException;

	ArrangementDTO getArrangementByPosition(long gameId, int position) throws HistoryNotFoundException, GameNotFoundException;

	Set<PointDTO> getAvailableMoves(long gameId, PointDTO selectedCell) throws GameNotFoundException, HistoryNotFoundException;

	ArrangementDTO applyMove(long gameId, MoveDTO dto) throws HistoryNotFoundException, GameNotFoundException;

	Game findAndCheckGame(long gameId) throws GameNotFoundException;

	void applyMirrorMove(long gameId, MoveDTO dto) throws GameNotFoundException;

	void applyFirstBotMove(long gameId) throws GameNotFoundException, HistoryNotFoundException;
}
