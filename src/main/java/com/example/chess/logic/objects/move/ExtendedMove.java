package com.example.chess.logic.objects.move;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.RatingParam;
import com.example.chess.enums.Side;
import com.example.chess.logic.objects.Rating;
import com.example.chess.logic.utils.ChessUtils;
import com.example.chess.logic.utils.CommonUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import static com.example.chess.logic.utils.CommonUtils.tabs;

@Log4j2
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuppressWarnings("WeakerAccess")
public class ExtendedMove implements Move {

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

    public boolean isPawnTransformation() {
        return ChessUtils.isPawnTransformation(this, getPieceFrom());
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

    public Side getSide() {
        return from.getSide();
    }

    public int getTotal() {
        return total;
    }

    @Override
    public String toString() {
        return CommonUtils.moveToString(this);
    }

    public void print(int tabsCount, String prefix) {
        print(tabsCount, prefix, "");
    }

    public void print(int tabsCount, String prefix, String postfix) {
        System.out.println(tabs(tabsCount) + prefix + "[" + this + "].total = " + getTotal() + postfix);
    }

    public void printRating(int tabsCount) {
        for (Rating rating : ratingMap.values()) {
            rating.print(tabsCount + 1);
        }
    }
}
