package com.example.chess.service;

import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.exceptions.HistoryNotFoundException;
import com.example.chess.service.support.CellsMatrix;

import java.util.Set;

public interface GameService {

    Game findAndCheckGame(long gameId) throws GameNotFoundException;

    ArrangementDTO createArrangementByGame(Game game, int position) throws HistoryNotFoundException;

    ArrangementDTO applyMove(Game gameId, MoveDTO dto) throws HistoryNotFoundException, GameNotFoundException;

    Set<PointDTO> getAvailableMoves(long gameId, PointDTO selectedCell) throws GameNotFoundException, HistoryNotFoundException;

    CellsMatrix createCellsMatrixByGame(Game game, int position) throws HistoryNotFoundException;
}
