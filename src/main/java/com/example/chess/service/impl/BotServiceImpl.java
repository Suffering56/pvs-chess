package com.example.chess.service.impl;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.*;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.BotService;
import com.example.chess.service.support.MoveHelper;
import lombok.extern.log4j.Log4j2;
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
    private CellsMatrix matrix;

    @Override
    @Profile
    public MoveDTO generateBotMove() {
        MoveHelper moveHelper = new MoveHelper(game, matrix);
        Side sideFrom = game.getPosition() % 2 == 0 ? Side.WHITE : Side.BLACK;

        Map<CellDTO, Set<PointDTO>> movesMap = matrix
                .filteredPiecesStream(sideFrom, PieceType.values())
                .collect(Collectors.toMap(Function.identity(), cellFrom -> moveHelper.getAvailableMoves(cellFrom.generatePoint())));

        List<MoveRatingDTO> ratingList = new ArrayList<>();
        for (CellDTO cellFrom : movesMap.keySet()) {
            Set<PointDTO> moves = movesMap.get(cellFrom);
            for (PointDTO pointTo : moves) {
                CellDTO attackedCell = matrix.getCell(pointTo);
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

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public void setCellsMatrix(CellsMatrix matrix) {
        this.matrix = matrix;
    }
}
