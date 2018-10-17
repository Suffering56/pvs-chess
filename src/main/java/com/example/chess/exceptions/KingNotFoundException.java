package com.example.chess.exceptions;

import com.example.chess.enums.Side;
import com.example.chess.service.support.CellsMatrix;
import com.example.chess.service.support.ExtendedMove;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KingNotFoundException extends RuntimeException {

    private final Side kingSide;
    private final CellsMatrix matrix;
    private CellsMatrix lastCorrectMatrix;
    private CellsMatrix originalMatrix;

    private ExtendedMove moveWhichKillKing;
    private ExtendedMove analyzedMove;

    public KingNotFoundException(Side kingSide, CellsMatrix matrix) {
        this.kingSide = kingSide;
        this.matrix = matrix;
    }

    public void print() {
        System.out.println("KingNotFoundException. KingSide = " + kingSide);

        System.out.println("analyzedMove = " + analyzedMove);
        System.out.println("OriginalMatrix");
        originalMatrix.print();
        System.out.println();
        
        System.out.println("moveWhichKillKing = " + moveWhichKillKing);
        System.out.println("lastCorrectMatrix");
        if (lastCorrectMatrix != null) {
            lastCorrectMatrix.print();
        }
        System.out.println();

        System.out.println("incorrectMatrix");
        matrix.print();
        System.out.println();
    }
}
