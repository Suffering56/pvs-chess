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
