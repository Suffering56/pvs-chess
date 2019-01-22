package com.example.chess.logic.objects.game;

import com.example.chess.enums.Piece;
import com.example.chess.enums.Side;
import com.example.chess.logic.objects.move.Move;
import com.example.chess.logic.utils.Immutable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.EnumMap;
import java.util.Map;

import static com.example.chess.logic.ChessConstants.ROOK_LONG_COLUMN_INDEX;
import static com.example.chess.logic.ChessConstants.ROOK_SHORT_COLUMN_INDEX;
import static com.example.chess.logic.utils.ChessUtils.isLongPawnMove;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FakeGame implements IGame, Immutable {

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

    @SuppressWarnings("WeakerAccess")
    public FakeGame executeMove(Move move, Piece pieceFrom) {
        return builder(this)
                .afterMove(move, pieceFrom)
                .build();
    }

    public static FakeGame ofGame(IGame game) {
        return builder(game)
                .build();
    }

    private static Builder builder(IGame game) {
        return new FakeGame().new Builder(game);
    }

    @SuppressWarnings("UnusedReturnValue")
    private class Builder {

        private Builder(IGame game) {
            for (Side side : Side.values()) {
                this.setPawnLongMoveColumnIndex(side, game.getPawnLongMoveColumnIndex(side))
                        .setLongCastlingAvailable(side, game.isLongCastlingAvailable(side))
                        .setShortCastlingAvailable(side, game.isShortCastlingAvailable(side));
            }
        }

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

        private Builder afterMove(Move move, Piece pieceFrom) {
            Side side = pieceFrom.getSide();

            int columnIndexFrom = move.getColumnIndexFrom();
            switch (pieceFrom.getType()) {
                case KING:
                    setLongCastlingAvailable(side, false);
                    setShortCastlingAvailable(side, false);
                    break;
                case ROOK:
                    if (columnIndexFrom == ROOK_SHORT_COLUMN_INDEX) {
                        setShortCastlingAvailable(side, false);
                    } else if (columnIndexFrom == ROOK_LONG_COLUMN_INDEX) {
                        setLongCastlingAvailable(side, false);
                    }
                    break;
                case PAWN:
                    if (isLongPawnMove(move, pieceFrom)) {
                        setPawnLongMoveColumnIndex(side, columnIndexFrom);
                    } else {
                        setPawnLongMoveColumnIndex(side, null);
                    }
                    break;
            }

            return this;
        }

        private FakeGame build() {
            return FakeGame.this;
        }
    }

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private class Features {
        boolean longCastlingAvailable = false;
        boolean shortCastlingAvailable = false;
        Integer pawnLongMoveColumnIndex = null;
        boolean isUnderCheck = false;
    }
}