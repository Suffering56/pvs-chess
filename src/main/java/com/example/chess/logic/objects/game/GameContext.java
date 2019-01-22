package com.example.chess.logic.objects.game;

import com.example.chess.enums.Piece;
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
public class GameContext {

    final RootGameContext root;
    final GameContext parent;
    final FakeGame game;
    final CellsMatrix matrix;
    final ExtendedMove lastMove;
    List<GameContext> children;

    public GameContext executeMove(ExtendedMove nextMove) {
        Piece pieceFrom = matrix.getCell(nextMove.getPointFrom()).getPiece();

        FakeGame nextGame = game.executeMove(nextMove, pieceFrom);
        CellsMatrix nextMatrix = matrix.executeMove(nextMove);

        return addChild(new GameContext(root, this, nextGame, nextMatrix, nextMove));
    }

    private GameContext addChild(GameContext childNode) {
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
        if (lastMove == null) {
            //if bot has first move in game
            return Side.BLACK;
        }
        return lastMove.getSide();
    }

    public boolean botNext() {
        return root == null || root.getBotSide() == nextTurnSide();
    }

    public boolean isRoot() {
        return root == null || parent == null;
    }
}
