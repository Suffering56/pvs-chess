package com.example.chess.service.support;

import com.example.chess.entity.Game;
import com.example.chess.enums.Side;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FakeGame implements Gameplay, Immutable {

    private final Map<Side, Features> featuresMap = new HashMap<Side, Features>() {{
        put(Side.WHITE, new Features());
        put(Side.BLACK, new Features());
    }};

    @Override
    public boolean isShortCastlingAvailable(Side side) {
        return featuresMap.get(side).isShortCastlingAvailable();
    }

    @Override
    public boolean isLongCastlingAvailable(Side side) {
        return featuresMap.get(side).isLongCastlingAvailable();
    }

    @Override
    public Integer getPawnLongMoveColumnIndex(Side side) {
        return featuresMap.get(side).getPawnLongMoveColumnIndex();
    }

    public Builder copy() {
        return builder(this);
    }

    public static Builder builder(Game game) {
        return builder((Gameplay) game);
    }

    private static Builder builder(Gameplay gameplay) {
        Builder builder = new FakeGame().new Builder();

        for (Side side : Side.values()) {
            builder.setPawnLongMoveColumnIndex(side, gameplay.getPawnLongMoveColumnIndex(side))
                    .setLongCastlingAvailable(side, gameplay.isLongCastlingAvailable(side))
                    .setShortCastlingAvailable(side, gameplay.isShortCastlingAvailable(side));
        }
        return builder;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public class Builder {

        public Builder setPawnLongMoveColumnIndex(Side side, Integer columnIndex) {
            Objects.requireNonNull(side);
            featuresMap.get(side).setPawnLongMoveColumnIndex(columnIndex);
            featuresMap.get(side.reverse()).setPawnLongMoveColumnIndex(null);
            return this;
        }

        public Builder setShortCastlingAvailable(Side side, boolean isAvailable) {
            Objects.requireNonNull(side);
            featuresMap.get(side).setShortCastlingAvailable(isAvailable);
            return this;
        }

        public Builder setLongCastlingAvailable(Side side, boolean isAvailable) {
            Objects.requireNonNull(side);
            featuresMap.get(side).setLongCastlingAvailable(isAvailable);
            return this;
        }

        public FakeGame build() {
            return FakeGame.this;
        }
    }

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private class Features {
        private boolean longCastlingAvailable = false;
        private boolean shortCastlingAvailable = false;
        private Integer pawnLongMoveColumnIndex = null;
        private boolean isUnderCheck = false;
    }
}