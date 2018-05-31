package com.example.chess.dto;

import lombok.*;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointDTO {

	protected Integer rowIndex;
	protected Integer columnIndex;

	public boolean isNotBorderedBy(PointDTO other) {
		if (equals(other)) {
			throw new RuntimeException("isBorderedBy self???");
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
}
