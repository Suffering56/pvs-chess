package com.example.chess.service.impl.bot;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.MoveDTO;
import com.example.chess.entity.Game;
import com.example.chess.service.support.BotMode;
import com.example.chess.service.support.CellsMatrix;
import com.example.chess.service.support.ExtendedMove;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Log4j2
@Service
@Qualifier(BotMode.RANDOM)
public class BotServiceImplRandom extends AbstractBotService {

    @Profile
    @Override
    public void applyBotMove(Game game) {
        CommonUtils.executeInSecondaryThread(() -> {
            CellsMatrix cellsMatrix = gameService.createCellsMatrixByGame(game, game.getPosition());
            MoveDTO moveDTO = findBestMove(game, cellsMatrix);
            gameService.applyMove(game, moveDTO);
        });
    }

    protected Consumer<? super ExtendedMove> calculateRating(Game game, CellsMatrix matrix) {
        return move -> {
        };
    }
}
