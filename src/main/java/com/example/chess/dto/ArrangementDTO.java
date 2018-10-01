package com.example.chess.dto;

import com.example.chess.enums.Side;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArrangementDTO {

	private int position;
	private List<List<CellDTO>> cellsMatrix;
	private Side underCheckSide;
}
