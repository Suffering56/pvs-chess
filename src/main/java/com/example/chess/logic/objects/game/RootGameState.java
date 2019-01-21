package com.example.chess.logic.objects.game;

import com.example.chess.enums.Side;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.ExtendedMove;
import lombok.Getter;

@Getter
public class RootGameState extends GameState {

    private final Side botSide;

    private RootGameState(FakeGame game, CellsMatrix matrix, ExtendedMove lastMove, Side botSide) {
        super(null, null, game, matrix, lastMove);
        this.botSide = botSide;
    }

    public static RootGameState of(FakeGame game, CellsMatrix matrix, ExtendedMove lastMove, Side botSide) {
        return new RootGameState(game, matrix, lastMove, botSide);
    }
}
