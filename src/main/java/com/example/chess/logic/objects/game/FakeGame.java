package com.example.chess.logic.objects.game;

import com.example.chess.entity.Game;
import com.example.chess.enums.Side;
import com.example.chess.logic.utils.Immutable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.EnumMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FakeGame implements Gameplay, Immutable {

    private final Map<Side, Features> featuresMap = new EnumMap<Side, Features>(Side.class) {{
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

        private Builder setPawnLongMoveColumnIndex(Side side, Integer columnIndex) {
            featuresMap.get(side).setPawnLongMoveColumnIndex(columnIndex);
            return this;
        }

        private Builder setShortCastlingAvailable(Side side, boolean isAvailable) {
            featuresMap.get(side).setShortCastlingAvailable(isAvailable);
            return this;
        }

        private Builder setLongCastlingAvailable(Side side, boolean isAvailable) {
            featuresMap.get(side).setLongCastlingAvailable(isAvailable);
            return this;
        }

        public FakeGame build() {
            return FakeGame.this;
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private class Features {
        boolean longCastlingAvailable = false;
        boolean shortCastlingAvailable = false;
        Integer pawnLongMoveColumnIndex = null;
        boolean isUnderCheck = false;
    }
}