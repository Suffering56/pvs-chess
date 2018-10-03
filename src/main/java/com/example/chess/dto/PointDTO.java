package com.example.chess.dto;

import com.example.chess.utils.CommonUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Objects;

/**
 * Immutable
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PointDTO {

    private final Integer rowIndex;
    private final Integer columnIndex;

    @JsonCreator
    public static PointDTO valueOf(@JsonProperty Integer rowIndex, @JsonProperty Integer columnIndex) {
        return new PointDTO(rowIndex, columnIndex);
    }

    public PointDTO setRowIndex(Integer rowIndex) {
        return new PointDTO(rowIndex, this.columnIndex);
    }

    public PointDTO setColumnIndex(Integer columnIndex) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointDTO pointDTO = (PointDTO) o;
        return Objects.equals(rowIndex, pointDTO.rowIndex) &&
                Objects.equals(columnIndex, pointDTO.columnIndex);
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
