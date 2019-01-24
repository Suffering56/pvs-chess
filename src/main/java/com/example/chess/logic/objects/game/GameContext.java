package com.example.chess.logic.objects.game;

import com.example.chess.dto.PointDTO;
import com.example.chess.enums.Piece;
import com.example.chess.enums.Side;
import com.example.chess.logic.MoveHelper;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.ExtendedMove;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.stream.Stream;

import static com.example.chess.logic.utils.CommonUtils.tabs;

@Log4j2
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class GameContext {

    final RootGameContext root;
    final GameContext parent;
    final FakeGame game;        //game state after lastMove

    final CellsMatrix matrix;   //matrix state after lastMove
    final ExtendedMove lastMove;
    //fk = pointFrom, sk = pointTo
    Map<PointDTO, List<GameContext>> children;

    public void fill(int deep) {
        MoveHelper.valueOf(this)
                .getStandardMovesStream(nextTurnSide())
                //здесь еще можно фильтровать по targetPoint (см - findMostProfitableMove)
                .sorted(Comparator.comparing(ExtendedMove::getValueFrom))   //TODO: кажется отсортировал
                .map(this::executeMove)
                .filter(childContext -> deep > 1)
                .forEach(childContext -> childContext.fill(deep - 1));
    }

    private GameContext executeMove(ExtendedMove nextMove) {
        Piece pieceFrom = matrix.getCell(nextMove.getPointFrom()).getPiece();

        FakeGame nextGame = game.executeMove(nextMove, pieceFrom);
        CellsMatrix nextMatrix = matrix.executeMove(nextMove);

        RootGameContext rootContext = isRoot() ? (RootGameContext) this : root;
        return addChild(new GameContext(rootContext, this, nextGame, nextMatrix, nextMove));
    }

    private GameContext addChild(GameContext childNode) {
        if (children == null) {
            children = new HashMap<>();
        }
        ExtendedMove childMove = childNode.getLastMove();

        List<GameContext> internalList = children.computeIfAbsent(childMove.getPointTo(), key -> new ArrayList<>());
        internalList.add(childNode);

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

    public boolean botLast() {
        return root == null || root.getBotSide() == lastMoveSide();
    }

    public boolean isRoot() {
        return root == null && parent == null;
    }

    public long getTotalMovesCount() {
        if (children == null) {
            return 1;
        }

        return childrenStream()
                .mapToLong(GameContext::getTotalMovesCount)
                .sum() + 1;
    }

    public Stream<GameContext> childrenStream() {
        return children.values()
                .stream()
                .flatMap(Collection::stream);
    }

    public Stream<GameContext> childrenStream(PointDTO targetPoint) {
        if (children == null) {
            return Stream.empty();
        }
        List<GameContext> internalList = children.get(targetPoint);
        if (internalList == null) {
            return Stream.empty();
        }
        return internalList.stream();
    }

    public int getDeep() {
        int deep = 0;
        GameContext parent = this.getParent();
        while (parent != null) {
            deep++;
            parent = parent.getParent();
        }

        return deep;
    }

    public void print() {
        int deep = getDeep();
        String prefix = tabs(deep);
        if (children != null) {
            log.info("{}context[{}].childrenCount: {}", prefix, deep, children.size());
            childrenStream().forEach(GameContext::print);
        }
    }

    public ExtendedMove getAnalyzedMove() {
        return lastMove;
    }

    public CellsMatrix getOriginalMatrix() {
        return root.getMatrix();
    }

    public CellsMatrix getPostAnalyzedMoveMatrix() {
        return matrix;
    }

    public boolean hasChildren() {
        return children != null;
    }
}
