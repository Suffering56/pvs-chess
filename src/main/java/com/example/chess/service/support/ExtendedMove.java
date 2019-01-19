package com.example.chess.service.support;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.utils.CommonUtils;
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
public class ExtendedMove {

    CellDTO from;
    CellDTO to;

    final Map<RatingParam, Rating> ratingMap = new EnumMap<>(RatingParam.class);
    int total = 0;

    public ExtendedMove(CellDTO from, CellDTO to) {
        this.from = from;
        this.to = to;
    }

    public PieceType getPieceFrom() {
        return from.getPieceType();
    }

    public PieceType getPieceTo() {
        return to.getPieceType();
    }

    public PointDTO getPointFrom() {
        return from.getPoint();
    }

    public PointDTO getPointTo() {
        return to.getPoint();
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

    public int getTotal() {
        return total;
    }

    public void updateTotalByGreedy() {
        int value = getPieceTo() != null ? getPieceTo().getValue() : 0;
        updateRating(Rating.builder().build(RatingParam.GREEDY, value));
    }

    public MoveDTO toMoveDTO() {
        //TODO: promotionPieceType can be not null
        return MoveDTO.valueOf(from.getPoint(), to.getPoint(), null);
    }

    public Side getSide() {
        return from.getSide();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Move[" + CommonUtils.moveToString(this) + "].total = " + getTotal() + "\r\n");
        for (Rating rating : ratingMap.values()) {
            result.append(rating);
        }

        return result.toString();
    }

    public boolean isPawnTransformation() {
        return getPieceFrom() == PieceType.PAWN && (getPointTo().getRowIndex() == 0 || getPointTo().getRowIndex() == 7);
    }
}
