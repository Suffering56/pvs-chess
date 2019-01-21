package com.example.chess.logic.objects.game;

import com.example.chess.enums.Side;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.ExtendedMove;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class GameState {

    final RootGameState root;
    final GameState parent;
    final FakeGame game;
    final CellsMatrix matrix;
    final ExtendedMove lastMove;
    List<GameState> children;

    public GameState executeMove(ExtendedMove nextMove) {
        FakeGame nextGameState = null;
        if (nextGameState == null) {
            throw new UnsupportedOperationException();
        }

        CellsMatrix nextMatrix = matrix.executeMove(nextMove);
        return addChild(new GameState(root, this, nextGameState, nextMatrix, nextMove));
    }

    private GameState addChild(GameState childNode) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(childNode);
        return childNode;
    }

    public int getPosition() {
        return matrix.getPosition();
    }

    public Side nextTurnSide() {
        return lastMoveSide().reverse();
    }

    public Side lastMoveSide() {
        return lastMove.getSide();
    }

    public boolean botNext() {
        return root == null || root.getBotSide() == nextTurnSide();
    }

    public boolean isRoot() {
        return root == null || parent == null;
    }
}
