package com.example.chess.service.impl.bot;

import com.example.chess.enums.Side;
import com.example.chess.service.support.BotMode;
import com.example.chess.service.support.CellsMatrix;
import com.example.chess.service.support.ExtendedMove;
import com.example.chess.service.support.FakeGame;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Log4j2
@Service
@Qualifier(BotMode.RANDOM)
public class BotServiceImplRandom extends AbstractBotService {

    @Override
    protected Consumer<? super ExtendedMove> calculateRating(FakeGame fakeGame, CellsMatrix originalMatrix, Side botSide, boolean isExternalCall) {
        return move -> {
        };
    }
}
