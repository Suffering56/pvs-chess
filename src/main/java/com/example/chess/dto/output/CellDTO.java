package com.example.chess.dto.output;

import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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
}
