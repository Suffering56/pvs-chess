package com.example.chess.entity;

import com.example.chess.enums.GameMode;
import com.example.chess.enums.Side;
import com.example.chess.logic.objects.game.IGame;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

import static com.example.chess.logic.ChessConstants.ROOK_LONG_COLUMN_INDEX;
import static com.example.chess.logic.ChessConstants.ROOK_SHORT_COLUMN_INDEX;

@Entity
@Getter
@Setter
@ToString
public class Game implements IGame {

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
    private void disableShortCasting(Side side) {
        featuresMap.get(side).setShortCastlingAvailable(false);
    }

    @Transient
    private void disableLongCasting(Side side) {
        featuresMap.get(side).setLongCastlingAvailable(false);
    }

    @Transient
    public void disableCasting(Side side) {
        disableLongCasting(side);
        disableShortCasting(side);
    }

    @Transient
    public void disableCasting(Side side, int rookColumnIndex) {
        if (rookColumnIndex == ROOK_SHORT_COLUMN_INDEX) {
            disableShortCasting(side);
        } else if (rookColumnIndex == ROOK_LONG_COLUMN_INDEX) {
            disableLongCasting(side);
        }
    }

    @Transient
    @Override
    public boolean isShortCastlingAvailable(Side side) {
        return featuresMap.get(side).getShortCastlingAvailable();
    }

    @Transient
    @Override
    public boolean isLongCastlingAvailable(Side side) {
        return featuresMap.get(side).getLongCastlingAvailable();
    }

    @Transient
    @Override
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
    private GameFeatures getWhiteFeatures() {
        return featuresMap.get(Side.WHITE);
    }

    @Transient
    private GameFeatures getBlackFeatures() {
        return featuresMap.get(Side.BLACK);
    }

    /**
     * Only works in AI mode
     */
    @Transient
    @JsonIgnore
    public Side getPlayerSide() {
        if (getMode() != GameMode.AI) {
            throw new RuntimeException("Game mode is not AI!");
        }

        Side selectedSide = null;
        if (getWhiteFeatures().getSessionId() != null && getBlackFeatures().getSessionId() == null) {
            selectedSide = Side.WHITE;
        }
        if (getBlackFeatures().getSessionId() != null && getWhiteFeatures().getSessionId() == null) {
            selectedSide = Side.BLACK;
        }

        return selectedSide;
    }

    /**
     * @return side, which has next move (not paused)
     */
    @Transient
    @JsonIgnore
    public Side getActiveSide() {
        return getPosition() % 2 == 0 ? Side.WHITE : Side.BLACK;
    }

    @Transient
    @JsonIgnore
    public void reset() {
        setPosition(0);
        clearFuturesMap();
        setUnderCheckSide(null);
    }

    public void clearFuturesMap() {
        setFeaturesMap(new HashMap<Side, GameFeatures>() {{
            put(Side.WHITE, new GameFeatures(Game.this, Side.WHITE));
            put(Side.BLACK, new GameFeatures(Game.this, Side.BLACK));
        }});
    }
}
