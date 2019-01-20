package com.example.chess.service.impl.bot;

import com.example.chess.enums.Side;
import com.example.chess.logic.debug.BotMode;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.ExtendedMove;
import com.example.chess.logic.objects.game.FakeGame;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@Log4j2
@Qualifier(BotMode.GREEDY)
public class BotServiceImplGreedy extends AbstractBotService {

    @Override
    protected Consumer<? super ExtendedMove> calculateRating(FakeGame fakeGame, CellsMatrix originalMatrix, Side botSide, boolean isExternalCall) {
        return ExtendedMove::updateTotalByGreedy;
    }

}
