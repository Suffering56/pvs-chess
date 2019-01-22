package com.example.chess.logic.objects.game;

import com.example.chess.enums.Side;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.ExtendedMove;
import lombok.Getter;

@Getter
public class RootGameContext extends GameContext {

    private final Side botSide;

    private RootGameContext(FakeGame game, CellsMatrix matrix, ExtendedMove lastMove, Side botSide) {
        super(null, null, game, matrix, lastMove);
        this.botSide = botSide;
    }

    public static RootGameContext of(FakeGame game, CellsMatrix matrix, ExtendedMove lastMove, Side botSide) {
        return new RootGameContext(game, matrix, lastMove, botSide);
    }
}
