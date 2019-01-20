package com.example.chess.dto;

import com.example.chess.logic.utils.Immutable;
import com.example.chess.logic.utils.CommonUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Objects;

import static com.example.chess.logic.ChessConstants.BOARD_SIZE;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PointDTO implements Immutable {

    int rowIndex;
    int columnIndex;

    private static PointDTO[][] pointsArray = new PointDTO[BOARD_SIZE][BOARD_SIZE];

    static {
        for (int rowIndex = 0; rowIndex < BOARD_SIZE; rowIndex++) {
            for (int columnIndex = 0; columnIndex < BOARD_SIZE; columnIndex++) {
                pointsArray[rowIndex][columnIndex] = new PointDTO(rowIndex, columnIndex);
            }
        }
    }

    @JsonCreator
    public static PointDTO valueOf(@JsonProperty int rowIndex, @JsonProperty int columnIndex) {
        return pointsArray[rowIndex][columnIndex];
    }

    public static boolean isCorrectIndex(int... indexes) {
        for (int index : indexes) {
            if (index < 0 || index >= 8) {
                return false;
            }
        }

        return true;
    }

    /**
     * Все point-ы лежат в pointsArray. В программе невозможно наличие point-ов которых нет в pointsArray.
     * Если у двух point-ов одинаковые координаты, значит это один и тот же point (если конечно не баловаться reflection).
     * Поэтому метод equals можно упростить. Тем более что используется он довольно часто.
     */
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowIndex, columnIndex);
    }

    @Override
    public String toString() {
        return CommonUtils.getColumnName(this) + (getRowIndex() + 1);
    }
}
