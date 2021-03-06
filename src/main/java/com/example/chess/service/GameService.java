package com.example.chess.service;

import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.entity.History;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.logic.objects.CellsMatrix;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public interface GameService {

    Game findAndCheckGame(long gameId) throws GameNotFoundException;

    ArrangementDTO createArrangementByGame(Game game, int position);

    Pair<CellsMatrix, ArrangementDTO> applyMove(Game gameId, MoveDTO dto) throws GameNotFoundException;

    Set<PointDTO> getAvailableMoves(long gameId, PointDTO selectedCell) throws GameNotFoundException;

    CellsMatrix createCellsMatrixByGame(Game game, int position);

    ArrangementDTO rollbackLastMove(Game game);

    History findLastMove(Game game);
}
