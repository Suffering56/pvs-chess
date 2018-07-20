package com.example.chess.dto;

import com.example.chess.enums.PieceType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MoveDTO {

	private PointDTO from;
	private PointDTO to;
	private PieceType pieceType;

	public MoveDTO(PointDTO from, PointDTO to) {
		this.from = from;
		this.to = to;
	}

	@JsonIgnore
	public MoveDTO getMirror() {
		MoveDTO mirror = new MoveDTO();

		mirror.setPieceType(pieceType);
		mirror.setFrom(from.getMirror());
		mirror.setTo(to.getMirror());
		return mirror;
	}
}
