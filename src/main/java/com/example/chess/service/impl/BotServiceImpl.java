package com.example.chess.service.impl;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.*;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.BotService;
import com.example.chess.service.GameService;
import com.example.chess.service.support.BotMode;
import com.example.chess.service.support.CellsMatrix;
import com.example.chess.service.support.MoveHelper;
import com.example.chess.service.support.MoveInfo;
import com.example.chess.service.support.api.MoveHelperAPI;
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

    private BotMode mode = BotMode.DEVELOP;

    public BotServiceImpl(GameService gameService) {
        this.gameService = gameService;
    }

    @Profile
    @Override
    public void applyBotMove(Game game) {
        CommonUtils.executeInSecondaryThread(() -> {
            CellsMatrix cellsMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());
            MoveDTO moveDTO = findBestMove(game, cellsMatrix);
            gameService.applyMove(game, moveDTO);
        });
    }

    private MoveDTO findBestMove(Game game, CellsMatrix matrix) {
        MoveHelperAPI moveHelper = new MoveHelper(game, matrix);
        Side sideFrom = game.getPosition() % 2 == 0 ? Side.WHITE : Side.BLACK;

        Map<CellDTO, Set<PointDTO>> movesMap = matrix
                .somePiecesStream(sideFrom, PieceType.values())
                .collect(Collectors.toMap(Function.identity(),
                        cellFrom -> moveHelper.getFilteredAvailableMoves(cellFrom.getPoint())));

        List<MoveInfo> infoList = new ArrayList<>();
        for (CellDTO cellFrom : movesMap.keySet()) {
            Set<PointDTO> moves = movesMap.get(cellFrom);
            for (PointDTO pointTo : moves) {
                CellDTO attackedCell = matrix.getCell(pointTo);
                MoveInfo moveInfo = new MoveInfo(cellFrom, pointTo, attackedCell.getPieceType());
                infoList.add(moveInfo);
            }
        }

        if (infoList.isEmpty()) {
            throw new RuntimeException("Game is end!");
        }

        //TODO: здесь можно применить... например стратегию(паттерн).
        MoveInfo bestMove;
        switch (mode) {
            case GREEDY:
                bestMove = getGreediestMove(infoList);
                break;
            case DEVELOP:
                bestMove = getDevelopMove(infoList);
                break;
            case RANDOM:
            default:
                bestMove = getRandomMove(infoList);
        }

        PieceType promotionPieceType = null;
        if (bestMove.getPieceFrom() == PieceType.PAWN && (bestMove.getTo().getRowIndex() == 0 || bestMove.getTo().getRowIndex() == 7)) {
            promotionPieceType = PieceType.QUEEN;
        }
        return MoveDTO.valueOf(bestMove.getFrom(), bestMove.getTo(), promotionPieceType);
    }

    private MoveInfo getDevelopMove(List<MoveInfo> infoList) {
        return getRandomMove(infoList);
    }

    private MoveInfo getGreediestMove(List<MoveInfo> infoList) {
        MoveInfo bestMove = null;
        int maxValue = 0;

        for (MoveInfo ratingDTO : infoList) {
            PieceType attackedPiece = ratingDTO.getAttackedPiece();
            if (attackedPiece != null) {
                if (attackedPiece.getValue() > maxValue) {
                    maxValue = attackedPiece.getValue();
                    bestMove = ratingDTO;
                }
            }
        }

        if (bestMove == null) {
            return getRandomMove(infoList);
        }

        return bestMove;
    }

    private MoveInfo getRandomMove(List<MoveInfo> infoList) {
        int i = (int) (infoList.size() * Math.random());
        return infoList.get(i);
    }
}
