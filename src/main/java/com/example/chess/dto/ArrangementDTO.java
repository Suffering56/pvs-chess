package com.example.chess.dto;

import com.example.chess.enums.Side;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArrangementDTO {

	private int position;
	private CellDTO[][] cellsMatrix;
	private Side underCheckSide;
}
