package com.example.chess.logic.objects.move;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.RatingParam;
import com.example.chess.enums.Side;
import com.example.chess.logic.objects.Rating;
import com.example.chess.logic.utils.CommonUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuppressWarnings("WeakerAccess")
public class ExtendedMove extends AbstractMove {

    final CellDTO from;
    final CellDTO to;

    final Map<RatingParam, Rating> ratingMap = new EnumMap<>(RatingParam.class);
    int total = 0;

    public ExtendedMove(CellDTO from, CellDTO to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public PointDTO getPointFrom() {
        return from.getPoint();
    }

    @Override
    public PointDTO getPointTo() {
        return to.getPoint();
    }

    @Override
    public PieceType getPieceFromPawn() {
        if (isPawnTransformation()) {
            return PieceType.QUEEN;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRowIndexFrom() {
        return from.getRowIndex();
    }

    @Override
    public int getColumnIndexFrom() {
        return from.getColumnIndex();
    }

    @Override
    public int getRowIndexTo() {
        return to.getRowIndex();
    }

    @Override
    public int getColumnIndexTo() {
        return to.getColumnIndex();
    }

    public PieceType getPieceFrom() {
        return from.getPieceType();
    }

    public PieceType getPieceTo() {
        return to.getPieceType();
    }

    public boolean isEmptyFrom() {
        return from.isEmpty();
    }

    public boolean isEmptyTo() {
        return to.isEmpty();
    }

    public boolean isHarmful() {
        return !isEmptyTo();
    }

    public boolean isHarmless() {
        return isEmptyTo();
    }

    private boolean isPawnTransformation() {
        return getPieceFrom() == PieceType.PAWN && (getPointTo().getRowIndex() == 0 || getPointTo().getRowIndex() == 7);
    }

    public int getValueFrom() {
        return Objects.requireNonNull(getPieceFrom()).getValue();
    }

    public int getValueTo() {
        return Objects.requireNonNull(getPieceTo()).getValue();
    }

    public int getValueTo(int defaultValue) {
        if (isEmptyTo()) {
            return defaultValue;
        }
        return getValueTo();
    }

    public void updateRating(Rating rating) {
        ratingMap.computeIfAbsent(rating.getParam(), key -> {
            total += rating.getValue() * rating.getParam().getFactor();
            return rating;
        });
    }

    public boolean hasDifferentPointTo(ExtendedMove otherMove) {
        return !hasSamePointTo(otherMove.getPointTo());
    }

    public boolean hasSamePointTo(PointDTO pointTo) {
        return this.getPointTo().equals(pointTo);
    }

    public void updateTotalByGreedy() {
        int value = getPieceTo() != null ? getPieceTo().getValue() : 0;
        updateRating(Rating.builder().build(RatingParam.GREEDY, value));
    }

    public Side getSide() {
        return from.getSide();
    }

    public int getTotal() {
        return total;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Move[" + CommonUtils.moveToString(this) + "].total = " + getTotal() + "\r\n");
        for (Rating rating : ratingMap.values()) {
            result.append(rating);
        }

        return result.toString();
    }
}
