package com.example.chess.entity;

import com.example.chess.enums.GameMode;
import com.example.chess.enums.Side;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Map;

@Entity
@Getter
@Setter
@ToString
public class Game {

	@Id
	@GenericGenerator(name = "game_id_seq", strategy = "sequence-identity", parameters = @org.hibernate.annotations.Parameter(name = "sequence", value = "game_id_seq"))
	@GeneratedValue(generator = "game_id_seq")
	private Long id;

	@ColumnDefault("0")
	@Column(nullable = false)
	private Integer position = 0;

	@Enumerated(EnumType.STRING)
	private GameMode mode;

	@OneToMany(mappedBy = "game", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@MapKey(name = "side")
	private Map<Side, GameFeatures> featuresMap;

	@Transient
	public void disableShortCasting(Side side) {
		featuresMap.get(side).setShortCastlingAvailable(false);
	}

	@Transient
	public void disableLongCasting(Side side) {
		featuresMap.get(side).setLongCastlingAvailable(false);
	}

	@Transient
	public boolean isShortCastlingAvailable(Side side) {
		return featuresMap.get(side).getShortCastlingAvailable();
	}

	@Transient
	public boolean isLongCastlingAvailable(Side side) {
		return featuresMap.get(side).getLongCastlingAvailable();
	}

	@Transient
	public Integer getPawnLongMoveColumnIndex(Side side) {
		return featuresMap.get(side).getPawnLongMoveColumnIndex();
	}

	@Transient
	public void setPawnLongMoveColumnIndex(Side side, Integer columnIndex) {
		featuresMap.get(side).setPawnLongMoveColumnIndex(columnIndex);
	}

	@Transient
	public GameFeatures getSideFeatures(Side side) {
		return featuresMap.get(side);
	}

	@Transient
	public Side getUnderCheckSide() {
		for (GameFeatures features : featuresMap.values()) {
			if (features.getIsUnderCheck()) {
				return features.getSide();
			}
		}
		return null;
	}

	@Transient
	public void setUnderCheckSide(Side side) {
		if (side != null) {
			featuresMap.get(side).setIsUnderCheck(true);
			featuresMap.get(side.reverse()).setIsUnderCheck(false);
		} else {
			getWhiteFeatures().setIsUnderCheck(false);
			getBlackFeatures().setIsUnderCheck(false);
		}
	}

	@Transient
	public GameFeatures getWhiteFeatures() {
		return featuresMap.get(Side.WHITE);
	}

	@Transient
	public GameFeatures getBlackFeatures() {
		return featuresMap.get(Side.BLACK);
	}
}
