package com.example.chess.service.impl.bot;

import com.example.chess.entity.Game;
import com.example.chess.service.support.BotMode;
import com.example.chess.service.support.CellsMatrix;
import com.example.chess.service.support.ExtendedMove;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@Log4j2
@Qualifier(BotMode.GREEDY)
public class BotServiceImplGreedy extends AbstractBotService {

    @Override
    protected Consumer<? super ExtendedMove> calculateRating(Game game, CellsMatrix matrix) {
        return ExtendedMove::applyGreedyMode;
    }

}
