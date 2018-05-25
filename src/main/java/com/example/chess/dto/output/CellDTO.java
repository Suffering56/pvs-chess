package com.example.chess.dto.output;

import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CellDTO extends PointDTO {

	private Side side;
	private PieceType piece;
	private boolean available;
	private boolean selected;

	public CellDTO(Integer rowIndex, Integer columnIndex) {
		super(rowIndex, columnIndex);
	}
}
