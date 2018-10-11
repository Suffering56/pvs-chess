package com.example.chess;

import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;

import java.util.HashMap;
import java.util.Map;

public class Debug {

    private static final boolean DESTINY_ENABLED = true;


    public static long availablePointsFound = 0;
    public static long getUnfilteredMovesCallsCount = 0;
    public static long moveHelpersCount = 0;
    public static long movesExecuted = 0;

    public static void resetCounters() {
        availablePointsFound = 0;
        getUnfilteredMovesCallsCount = 0;
        moveHelpersCount = 0;
        movesExecuted = 0;
    }

    public static void printCounters() {
        System.out.println("moveHelpersCount = " + moveHelpersCount);
        System.out.println("movesExecuted = " + movesExecuted);
        System.out.println("availablePointsFound = " + availablePointsFound);
        System.out.println("getUnfilteredMovesCallsCount = " + getUnfilteredMovesCallsCount);
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
