package com.example.chess.entity;

import com.example.chess.enums.Side;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class Game {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ColumnDefault("0")
	@Column(nullable = false)
	private Integer position = 0;

	private String whiteSessionId;

	private String blackSessionId;

	@ColumnDefault("true")
	@Column(nullable = false)
	private Boolean isWhiteLongCastlingAvailable = true;

	@ColumnDefault("true")
	@Column(nullable = false)
	private Boolean isWhiteShortCastlingAvailable = true;

	@ColumnDefault("true")
	@Column(nullable = false)
	private Boolean isBlackLongCastlingAvailable = true;

	@ColumnDefault("true")
	@Column(nullable = false)
	private Boolean isBlackShortCastlingAvailable = true;

	public void disableShortCasting(Side side) {
		if (side == Side.white) {
			setIsWhiteShortCastlingAvailable(false);
		} else {
			setIsBlackShortCastlingAvailable(false);
		}
	}

	public void disableLongCasting(Side side) {
		if (side == Side.white) {
			setIsWhiteLongCastlingAvailable(false);
		} else {
			setIsBlackLongCastlingAvailable(false);
		}
	}

	public boolean isShortCastlingAvailableForSide(Side side) {
		if (side == Side.white) {
			return getIsWhiteShortCastlingAvailable();
		} else {
			return getIsBlackShortCastlingAvailable();
		}
	}

	public boolean isLongCastlingAvailableForSide(Side side) {
		if (side == Side.white) {
			return getIsWhiteLongCastlingAvailable();
		} else {
			return getIsBlackLongCastlingAvailable();
		}
	}
}
