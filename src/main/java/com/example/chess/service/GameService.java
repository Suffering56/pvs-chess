package com.example.chess.service;

import com.example.chess.dto.PointDTO;
import com.example.chess.dto.input.MoveDTO;
import com.example.chess.dto.output.ArrangementDTO;
import com.example.chess.entity.Game;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.exceptions.HistoryNotFoundException;

import java.util.List;

public interface GameService {

	ArrangementDTO getArrangementByPosition(long gameId, int position) throws HistoryNotFoundException;

	List<PointDTO> getAvailableMoves(long gameId, PointDTO selectedCell) throws GameNotFoundException, HistoryNotFoundException;

	ArrangementDTO applyMove(long gameId, MoveDTO dto) throws HistoryNotFoundException, GameNotFoundException;

	Game findAndCheckGame(long gameId) throws GameNotFoundException;
}
