package com.example.chess.service.support;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;

@Getter
@Setter
public class ExtendedMove {

    private CellDTO from;
    private CellDTO to;

    private Map<RatingParam, Integer> ratingMap = new HashMap<>();
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
        return Objects.requireNonNull(from.getPieceType()).getValue();
    }

    public int getValueTo() {
        return Objects.requireNonNull(to.getPieceType()).getValue();
    }

    public Integer getExchangeDiff() {
        if (isEmptyTo()) {
            return null;
        }
        return getValueTo() - getValueFrom();
    }

    public void updateRatingByParam(RatingParam param) {
        updateRatingByParam(param, 1);
    }

    public void updateRatingByParam(RatingParam param, Integer value) {
        if (value == null || value == 0) {
            return;
        }

        ratingMap.put(param, value);
        total += value * param.getFactor();
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
}
