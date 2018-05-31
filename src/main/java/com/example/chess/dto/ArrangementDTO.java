package com.example.chess.dto;

import com.example.chess.enums.Side;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArrangementDTO {

	private int position;
	private List<List<CellDTO>> cellsMatrix;
	private Side underCheckSide;
}
