package com.example.chess.service.impl;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveRatingDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
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

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    private Function<Game, Function<List<List<CellDTO>>, MoveService>> moveServiceFactory;

    @Override
    @Profile
    public void applyBotMove() {
        log.info("applyBotMove");
        log.info("game: " + game);
        log.info("cellsMatrix: " + cellsMatrix);
        log.info("moveServiceFactory: " + moveServiceFactory);
        
        MoveService moveServiceImpl = moveServiceFactory.apply(game).apply(cellsMatrix);
        Side sideFrom = game.getPosition() % 2 == 0 ? Side.WHITE : Side.BLACK;

        Map<CellDTO, Set<PointDTO>> movesMap = moveServiceImpl
                .filteredPiecesStream(sideFrom, PieceType.values())
                .collect(Collectors.toMap(Function.identity(), cellFrom -> moveServiceImpl.getAvailableMoves(cellFrom.generatePoint())));

        log.info("movesMap.size: " + movesMap.size());

        List<MoveRatingDTO> ratingList = new ArrayList<>();

        for (CellDTO cellFrom : movesMap.keySet()) {
            Set<PointDTO> moves = movesMap.get(cellFrom);
            for (PointDTO pointTo : moves) {
                CellDTO attackedCell = cellsMatrix.get(pointTo.getRowIndex()).get(pointTo.getColumnIndex());
                MoveRatingDTO moveRatingDTO = new MoveRatingDTO(cellFrom, pointTo, attackedCell.getPieceType());
                ratingList.add(moveRatingDTO);
            }
        }

        log.info("ratingList.size: " + ratingList.size());
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
