package com.example.chess.logic.objects.game;

import com.example.chess.entity.Game;
import com.example.chess.enums.Side;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.ExtendedMove;
import lombok.Getter;

@Getter
public class RootGameContext extends GameContext {

    private final Side botSide;

    private RootGameContext(Game game, CellsMatrix matrix, ExtendedMove lastMove, Side botSide) {
        super(null, null, FakeGame.ofGame(game), matrix, lastMove);
        this.botSide = botSide;
    }

    public static RootGameContext of(Game game, CellsMatrix matrix, ExtendedMove lastMove, Side botSide) {
        return new RootGameContext(game, matrix, lastMove, botSide);
    }

    @Override
    public long getTotalMovesCount() {
        return super.getTotalMovesCount() - 1;
    }

    @Override
    public int getDeep() {
        return 0;
    }
}
