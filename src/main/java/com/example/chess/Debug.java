package com.example.chess;

import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class Debug {

    private static final boolean DESTINY_ENABLED = false;
    public static final boolean IS_PARALLEL = false;

    private static final AtomicLong availablePointsFound = new AtomicLong(0);
    private static final AtomicLong addMovesForCallsCount = new AtomicLong(0);
    private static final AtomicLong moveHelpersCount = new AtomicLong(0);
    private static final AtomicLong movesExecuted = new AtomicLong(0);

    public static void incrementAvailablePointsFound(int pointsCount) {
        increment(availablePointsFound, pointsCount);
    }

    public static void incrementAddMovesForCallsCount() {
        increment(addMovesForCallsCount);
    }

    public static void incrementMoveHelpersCount() {
        increment(moveHelpersCount);
    }


    public static void incrementMovesExecuted() {
        increment(movesExecuted);
    }

    private static void increment(AtomicLong atomicLong) {
        increment(atomicLong, 1);
    }

    private static void increment(AtomicLong atomicLong, int value) {
        while (true) {
            long existingValue = atomicLong.get();
            long newValue = existingValue + value;
            if (atomicLong.compareAndSet(existingValue, newValue)) {
                return;
            }
        }
    }

    public static void resetCounters() {
        availablePointsFound.set(0);
        addMovesForCallsCount.set(0);
        moveHelpersCount.set(0);
        movesExecuted.set(0);
    }

    public static void printCounters() {
        System.out.println("moveHelpersCount = " + moveHelpersCount.get());
        System.out.println("movesExecuted = " + movesExecuted.get());
        System.out.println("availablePointsFound = " + availablePointsFound.get());
        System.out.println("addMovesForCallsCount = " + addMovesForCallsCount.get());
    }


    private static Map<Integer, MoveDTO> destinyMap = new HashMap<Integer, MoveDTO>() {{
        // "e7---e6"
        put(1, MoveDTO.valueOf(PointDTO.valueOf(6, 3), PointDTO.valueOf(5, 3), null));
        // "Bf8---b4"
        put(3, MoveDTO.valueOf(PointDTO.valueOf(7, 2), PointDTO.valueOf(3, 6), null));
    }};

    public static MoveDTO getPredestinedMove(int position) {
        if (Debug.DESTINY_ENABLED) {
            return destinyMap.get(position);
        }

        return null;
    }
}
