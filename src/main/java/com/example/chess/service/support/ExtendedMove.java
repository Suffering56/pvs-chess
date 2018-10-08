package com.example.chess.service.support;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.utils.CommonUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
public class ExtendedMove {

    private CellDTO from;
    private CellDTO to;

    private final Map<RatingParam, Rating> ratingMap = new HashMap<>();
    private int total = 0;

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

    public int getValueFrom(int defaultValue) {
        if (isEmptyFrom()) {
            return defaultValue;
        }
        return getValueFrom();
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

    public int getExchangeDiff() {
        if (isEmptyTo()) {
            return 0;
        }
        return getValueTo() - getValueFrom();
    }

    public void updateRating(Rating rating) {
        if (ratingMap.containsKey(rating.getParam())) {
            throw new RuntimeException("Rating already exist");
        }
        ratingMap.put(rating.getParam(), rating);

        total += rating.getValue() * rating.getParam().getFactor();
    }

    public boolean hasDifferentPointTo(ExtendedMove otherMove) {
        return !this.getPointTo().equals(otherMove.getPointTo());
    }

    public int getTotal() {
        return total;
    }

    public void applyGreedyMode() {
        total = getPieceTo() != null ? getPieceTo().getValue() : 0;
    }

    public MoveDTO toMoveDTO() {
        //TODO: promotionPieceType can be not null
        return MoveDTO.valueOf(from.getPoint(), to.getPoint(), null);
    }

    public boolean isVertical() {
        return from.getColumnIndex().equals(to.getColumnIndex());
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Move[" + CommonUtils.moveToString(this) + "].total = " + total + "\r\n");
        for (Rating rating : ratingMap.values()) {
            result.append(rating);
        }

        return result.toString();
    }

}
