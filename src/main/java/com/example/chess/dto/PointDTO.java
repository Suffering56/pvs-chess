package com.example.chess.dto;

import com.example.chess.service.support.Immutable;
import com.example.chess.utils.CommonUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Objects;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PointDTO implements Immutable {

    private final int rowIndex;
    private final int columnIndex;

    private static PointDTO[][] pointsArray = new PointDTO[8][8];

    static {
        for (int rowIndex = 0; rowIndex < 8; rowIndex++) {
            for (int columnIndex = 0; columnIndex < 8; columnIndex++) {
                pointsArray[rowIndex][columnIndex] = new PointDTO(rowIndex, columnIndex);
            }
        }
    }

    @JsonCreator
    public static PointDTO valueOf(@JsonProperty int rowIndex, @JsonProperty int columnIndex) {
        return pointsArray[rowIndex][columnIndex];
    }

    public static boolean isCorrectIndex(int index) {
        return index >= 0 && index < 8;
    }

    public static boolean isCorrectIndex(int... indexes) {
        for (int index : indexes) {
            if (index < 0 || index >= 8) {
                return false;
            }
        }

        return true;
    }

    public PointDTO setRowIndex(int rowIndex) {
        return new PointDTO(rowIndex, this.columnIndex);
    }

    public PointDTO setColumnIndex(int columnIndex) {
        return new PointDTO(this.rowIndex, columnIndex);
    }

    @JsonIgnore
    public boolean isNotBorderedBy(PointDTO other) {
        if (equals(other)) {
            throw new RuntimeException("invoked: isNotBorderedBy(SELF!!!)");
        }

        int rowDiff = Math.abs(rowIndex - other.rowIndex);
        int columnDiff = Math.abs(columnIndex - other.columnIndex);
        int diff = rowDiff + columnDiff;

        if (diff == 1) {
            return false;
        } else {
            return diff != 2 || rowDiff != columnDiff;
        }
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
