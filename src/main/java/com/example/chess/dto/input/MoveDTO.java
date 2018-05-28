package com.example.chess.dto.input;

import com.example.chess.dto.PointDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MoveDTO {

	private PointDTO from;
	private PointDTO to;
}
