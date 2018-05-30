package com.example.chess.dto;

import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class PointDTO {

	protected Integer rowIndex;
	protected Integer columnIndex;

	public boolean isBorderedBy(PointDTO other) {
		if (equals(other)) {
			throw new RuntimeException("isBorderedBy self???");
		}

		int diff = Math.abs(rowIndex - other.rowIndex) +  Math.abs(columnIndex - other.columnIndex);
		log.info("diff[{}.{}]: " + diff, other.rowIndex, other.columnIndex);

		return Math.abs(rowIndex - other.rowIndex) +  Math.abs(columnIndex - other.columnIndex) == 1;
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
