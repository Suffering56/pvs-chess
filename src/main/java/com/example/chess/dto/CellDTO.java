package com.example.chess.dto;

import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class CellDTO {

	private Piece piece;

	protected Integer rowIndex;
	protected Integer columnIndex;

	private boolean available;
	private boolean selected;

	public CellDTO(Integer rowIndex, Integer columnIndex) {
		this.rowIndex = rowIndex;
		this.columnIndex = columnIndex;
	}

	public Side getPieceSide() {
		if (getPiece() == null) {
			return null;
		}
		return getPiece().getSide();
	}

	public PieceType getPieceType() {
		if (getPiece() == null) {
			return null;
		}
		return getPiece().getType();
	}

	public PointDTO generatePoint() {
		return new PointDTO(rowIndex, columnIndex);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null || getClass() != o.getClass()) return false;

		CellDTO cellDTO = (CellDTO) o;

		return new EqualsBuilder()
				.append(available, cellDTO.available)
				.append(selected, cellDTO.selected)
				.append(piece, cellDTO.piece)
				.append(rowIndex, cellDTO.rowIndex)
				.append(columnIndex, cellDTO.columnIndex)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(piece)
				.append(rowIndex)
				.append(columnIndex)
				.append(available)
				.append(selected)
				.toHashCode();
	}
}
