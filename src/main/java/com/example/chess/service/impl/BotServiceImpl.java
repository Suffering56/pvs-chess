package com.example.chess.service.impl;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.MoveRatingDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.BotService;
import com.example.chess.service.MoveService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Log4j2
public class BotServiceImpl implements BotService {

    private Game game;
    private List<List<CellDTO>> cellsMatrix;
    private Function<Game, Function<List<List<CellDTO>>, MoveService>> moveServiceFactory;

    @Override
    @Profile
    public MoveDTO generateBotMove() {
        MoveService moveServiceImpl = moveServiceFactory.apply(game).apply(cellsMatrix);
        Side sideFrom = game.getPosition() % 2 == 0 ? Side.WHITE : Side.BLACK;

        Map<CellDTO, Set<PointDTO>> movesMap = moveServiceImpl
                .filteredPiecesStream(sideFrom, PieceType.values())
                .collect(Collectors.toMap(Function.identity(), cellFrom -> moveServiceImpl.getAvailableMoves(cellFrom.generatePoint())));

        List<MoveRatingDTO> ratingList = new ArrayList<>();
        for (CellDTO cellFrom : movesMap.keySet()) {
            Set<PointDTO> moves = movesMap.get(cellFrom);
            for (PointDTO pointTo : moves) {
                CellDTO attackedCell = cellsMatrix.get(pointTo.getRowIndex()).get(pointTo.getColumnIndex());
                MoveRatingDTO moveRatingDTO = new MoveRatingDTO(cellFrom, pointTo, attackedCell.getPieceType());
                ratingList.add(moveRatingDTO);
            }
        }

        if (ratingList.isEmpty()) {
            throw new RuntimeException("Game is end!");
        }

        MoveRatingDTO bestMove = null;
        int maxValue = 0;
        for (MoveRatingDTO ratingDTO : ratingList) {
            PieceType attackedPiece = ratingDTO.getAttackedPiece();
            if (attackedPiece != null) {
                if (attackedPiece.getValue() > maxValue) {
                    maxValue = attackedPiece.getValue();
                    bestMove = ratingDTO;
                }
            }
        }

        if (bestMove == null) {
            int i = (int) (ratingList.size() * Math.random());
            bestMove = ratingList.get(i);
        }

        return new MoveDTO(bestMove.getCellFrom().generatePoint(), bestMove.getPointTo());
    }

    @Autowired
    public void setMoveServiceFactory(Function<Game, Function<List<List<CellDTO>>, MoveService>> moveServiceFactory) {
        this.moveServiceFactory = moveServiceFactory;
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public void setCellsMatrix(List<List<CellDTO>> cellsMatrix) {
        this.cellsMatrix = cellsMatrix;
    }
}
