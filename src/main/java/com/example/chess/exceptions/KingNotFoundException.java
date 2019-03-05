package com.example.chess.exceptions;

import com.example.chess.enums.Side;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.ExtendedMove;
import com.example.chess.logic.utils.CommonUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Getter
@Setter
@Log4j2
public class KingNotFoundException extends RuntimeException {

    private Side kingSide;
    private CellsMatrix originalMatrix;
    private ExtendedMove analyzedMove;
    private CellsMatrix prevMatrix;
    private ExtendedMove prevMove;
    private CellsMatrix errorMatrix;

    public KingNotFoundException(CellsMatrix errorMatrix, Side kingSide) {
        this.errorMatrix = errorMatrix;
        this.kingSide = kingSide;
    }

    public void print() {
        log.info("kingSide = " + kingSide);
        log.info("");
        log.info("ORIGINAL_MATRIX:");
        originalMatrix.print();

        log.info("analyzedMove = " + CommonUtils.moveToString(analyzedMove));
        log.info("");

        log.info("PREV_MATRIX:");
        prevMatrix.print();

        log.info("prevMove = " + CommonUtils.moveToString(prevMove));
        log.info("");

        log.info("ERROR_MATRIX:");
        errorMatrix.print();

        log.error(getMessage(), this);
    }
}
