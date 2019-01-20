package com.example.chess.service.impl;

import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.entity.History;
import com.example.chess.enums.Piece;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.repository.GameRepository;
import com.example.chess.repository.HistoryRepository;
import com.example.chess.service.GameService;
import com.example.chess.service.support.CellsMatrix;
import com.example.chess.service.support.MoveHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Log4j2
@Service
public class GameServiceImpl implements GameService {

    private final GameRepository gameRepository;
    private final HistoryRepository historyRepository;

    public GameServiceImpl(GameRepository gameRepository, HistoryRepository historyRepository) {
        this.gameRepository = gameRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    public ArrangementDTO createArrangementByGame(Game game, int position) {
        return createCellsMatrixByGame(game, position).generateArrangement(game.getUnderCheckSide());
    }

    @Override
    @Transactional
    public ArrangementDTO applyMove(Game game, MoveDTO move) {
        CellsMatrix prevMatrix = createCellsMatrixByGame(game, game.getPosition());

        Piece pieceFrom = prevMatrix.getCell(move.getFrom()).getPiece();
        Side sideFrom = pieceFrom.getSide();

        game.setPawnLongMoveColumnIndex(sideFrom, null);
        game.setUnderCheckSide(null);

        switch (pieceFrom.getType()) {
            case KING:
                game.disableCasting(sideFrom);
                break;
            case ROOK:
                int rookColumnIndex = move.getFrom().getColumnIndex();
                game.disableCasting(sideFrom, rookColumnIndex);
                break;
            case PAWN:
                if (move.isLongPawnMove()) {
                    //it needs for handling of the en-passant
                    game.setPawnLongMoveColumnIndex(sideFrom, move.getFrom().getColumnIndex());
                }
                break;
        }

        Side enemySide = sideFrom.reverse();
        CellsMatrix nextMatrix = prevMatrix.executeMove(move).getNewMatrix();

        if (MoveHelper.valueOf(game.toFakeBuilder().build(), nextMatrix).isKingUnderAttack(enemySide)) {
            /*
                Если данный ход объявил шах вражескому королю, то нужно подсветить вражеского короля на доске.
                А еще этот параметр (game.underCheckSide) используется при вычислении доступных ходов,
                т.к. если король под атакой - то далеко не каждой фигурой можно будет ходить.
             */
            game.setUnderCheckSide(enemySide);
        }

        game.setPosition(nextMatrix.getPosition());
        game.getSideFeatures(sideFrom).setLastVisitDate(LocalDateTime.now());

        historyRepository.save(move.toHistory(game.getId(), nextMatrix.getPosition(), pieceFrom));
        gameRepository.save(game);

        return nextMatrix.generateArrangement(game.getUnderCheckSide());
    }

    @Override
    public Game findAndCheckGame(long gameId) throws GameNotFoundException {
        return gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
    }

    @Override
    public Set<PointDTO> getAvailableMoves(long gameId, PointDTO point) throws GameNotFoundException {
        Game game = findAndCheckGame(gameId);
        CellsMatrix matrix = createCellsMatrixByGame(game, game.getPosition());
        return MoveHelper.valueOf(game.toFakeBuilder().build(), matrix).getFilteredAvailablePoints(point);
    }

    @Override
    public CellsMatrix createCellsMatrixByGame(Game game, int position) {
        List<History> historyList = findHistoryByGameIdAndPosition(game, position);
        return CellsMatrix.ofHistory(position, historyList);
    }

    private List<History> findHistoryByGameIdAndPosition(Game game, int position) {
        return historyRepository.findByGameIdAndPositionLessThanEqualOrderByPositionAsc(game.getId(), position);
    }

}
