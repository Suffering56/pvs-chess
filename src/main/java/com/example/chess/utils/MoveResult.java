package com.example.chess.utils;

import com.example.chess.service.support.CellsMatrix;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Immutable: TODO: make CellsMatrix Immutable
 */
@Getter
@AllArgsConstructor
public class MoveResult {

    private final CellsMatrix prevMatrix;
    private final CellsMatrix newMatrix;
}
