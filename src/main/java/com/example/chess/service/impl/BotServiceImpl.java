package com.example.chess.service.impl;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.*;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import com.example.chess.service.support.CellsMatrix;
import com.example.chess.service.support.MoveHelper;
import com.example.chess.service.support.MoveRating;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
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

    private final GameService gameService;
    @Value("${app.game.bot.move-delay}")
    private Long botMoveDelay;

    public BotServiceImpl(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    @Profile
    public void applyBotMove(Game game) {
        CommonUtils.executeInSecondaryThread(() -> {
            CellsMatrix cellsMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());
            MoveDTO moveDTO = findBestMove(game, cellsMatrix);
            gameService.applyMove(game, moveDTO);
        });
    }

    private MoveDTO findBestMove(Game game, CellsMatrix matrix) {
        MoveHelper moveHelper = MoveHelper.valueOf(game, matrix);
        Side sideFrom = game.getPosition() % 2 == 0 ? Side.WHITE : Side.BLACK;

        Map<CellDTO, Set<PointDTO>> movesMap = matrix
                .somePiecesStream(sideFrom, PieceType.values())
                .collect(Collectors.toMap(Function.identity(),
                        cellFrom -> moveHelper.getFilteredAvailableMoves(cellFrom.getPoint())));

        List<MoveRating> ratingList = new ArrayList<>();
        for (CellDTO cellFrom : movesMap.keySet()) {
            Set<PointDTO> moves = movesMap.get(cellFrom);
            for (PointDTO pointTo : moves) {
                CellDTO attackedCell = matrix.getCell(pointTo);
                MoveRating moveRating = new MoveRating(cellFrom, pointTo, attackedCell.getPieceType());
                ratingList.add(moveRating);
            }
        }

        if (ratingList.isEmpty()) {
            throw new RuntimeException("Game is end!");
        }

        MoveRating maxRating = null;
        int maxValue = 0;
        for (MoveRating ratingDTO : ratingList) {
            PieceType attackedPiece = ratingDTO.getAttackedPiece();
            if (attackedPiece != null) {
                if (attackedPiece.getValue() > maxValue) {
                    maxValue = attackedPiece.getValue();
                    maxRating = ratingDTO;
                }
            }
        }

        if (maxRating == null) {
            int i = (int) (ratingList.size() * Math.random());
            maxRating = ratingList.get(i);
        }

        PieceType promotionPieceType = null;
        if (maxRating.getPieceFrom() == PieceType.PAWN && (maxRating.getTo().getRowIndex() == 0 || maxRating.getTo().getRowIndex() == 7)) {
            promotionPieceType = PieceType.QUEEN;
        }

        return MoveDTO.valueOf(maxRating.getFrom(), maxRating.getTo(), promotionPieceType);
    }
}
