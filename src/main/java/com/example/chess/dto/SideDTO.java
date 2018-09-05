package com.example.chess.dto;

import com.example.chess.enums.Side;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SideDTO {

	public static String VIEWER = "VIEWER";

	private String side;

	public SideDTO(Side side) {
		this.side = side.name();
	}

	@JsonIgnore
	public Side getSideAsEnum() {
		return Side.valueOf(side);
	}

	public static SideDTO createUnselected() {
		return new SideDTO();
	}
}
