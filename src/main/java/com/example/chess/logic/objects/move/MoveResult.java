package com.example.chess.logic.objects.move;

import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.utils.Immutable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MoveResult implements Immutable {

    private final CellsMatrix prevMatrix;
    private final CellsMatrix newMatrix;

    public static MoveResult valueOf(CellsMatrix prevMatrix, CellsMatrix newMatrix) {
        return new MoveResult(prevMatrix, newMatrix);
    }
}
