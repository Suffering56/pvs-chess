package com.example.chess.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ParamsPlayerDTO {

	private Boolean isWhite;
	private Boolean isViewer = false;
}
