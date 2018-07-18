package com.example.chess.entity;

import com.example.chess.enums.GameMode;
import com.example.chess.enums.Side;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
public class Game {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ColumnDefault("0")
	@Column(nullable = false)
	private Integer position = 0;

	@Enumerated(EnumType.STRING)
	private GameMode mode;

	private String whiteSessionId;

	private String blackSessionId;

	private LocalDateTime whiteLastVisitDate;

	private LocalDateTime blackLastVisitDate;

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

	//если пешка сделала длинный ход (на 2 клетки вперед) здесь храним индекс
	private Integer whitePawnLongMoveColumnIndex;
	private Integer blackPawnLongMoveColumnIndex;

	@Enumerated(EnumType.STRING)
	private Side underCheckSide;

	@Transient
	public void disableShortCasting(Side side) {
		if (side == Side.white) {
			setIsWhiteShortCastlingAvailable(false);
		} else {
			setIsBlackShortCastlingAvailable(false);
		}
	}

	@Transient
	public void disableLongCasting(Side side) {
		if (side == Side.white) {
			setIsWhiteLongCastlingAvailable(false);
		} else {
			setIsBlackLongCastlingAvailable(false);
		}
	}

	@Transient
	public boolean isShortCastlingAvailable(Side side) {
		if (side == Side.white) {
			return getIsWhiteShortCastlingAvailable();
		} else {
			return getIsBlackShortCastlingAvailable();
		}
	}

	@Transient
	public boolean isLongCastlingAvailable(Side side) {
		if (side == Side.white) {
			return getIsWhiteLongCastlingAvailable();
		} else {
			return getIsBlackLongCastlingAvailable();
		}
	}

	@Transient
	public Integer getPawnLongMoveColumnIndex(Side side) {
		if (side == Side.white) {
			return getWhitePawnLongMoveColumnIndex();
		} else {
			return getBlackPawnLongMoveColumnIndex();
		}
	}
	@Transient

	public void setPawnLongMoveColumnIndex(Side side, Integer columnIndex) {
		if (side == Side.white) {
			setWhitePawnLongMoveColumnIndex(columnIndex);
		} else {
			setBlackPawnLongMoveColumnIndex(columnIndex);
		}
	}
}
