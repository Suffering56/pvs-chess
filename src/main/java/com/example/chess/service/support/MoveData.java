package com.example.chess.service.support;

import com.example.chess.enums.Side;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class MoveData {

    @Getter
    private final int deep;
    @Getter
    private final ExtendedMove executedMove;
    @Getter
    private final MoveResult moveExecutionResult;     //the chessboard state after executedMove completion

    @Setter
    @Getter
//    private Map<PointDTO, MoveData> moreDeepMoves;   //key = targetPoint (value.getExecutedMove().getPointTo();
    private List<MoveData> moreDeepMoves;   //key = targetPoint (value.getExecutedMove().getPointTo();

    public MoveData(ExtendedMove executedMove, MoveResult moveExecutionResult, int deep) {
        this.executedMove = executedMove;
        this.moveExecutionResult = moveExecutionResult;
        this.deep = deep;
    }

    public CellsMatrix getNextMatrix() {
        return moveExecutionResult.getNewMatrix();
    }

    public Side getExecutedMoveSide() {
        return executedMove.getFrom().getSide();
    }

    private String whoIsMove() {
        if (deep % 2 == 0) {
            return "player";
        } else {
            return "bot";
        }
    }
}
