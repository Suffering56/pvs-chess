package com.example.chess.service.support;

import lombok.Getter;

@Getter
public enum RatingParam {
    /**
     * Учитывается разница при размене фигур. Данная проверка ничего не знает о том, сможет ли противник вообще
     * срубить нашу фигуру после взятия. Т.е. мы просто сравниваем фигуры, участвующие в размене.
     * Если срубленная фигура лучше рубящей - то разница будет положительной. Если хуже - отрицательной.
     * Если ценность фигур равна - то разница будет равна нулю.
     * <p>
     * Формула: additionalRating = pieceTo.value - pieceFrom.value
     */
    EXCHANGE_DIFF(100),
    /**
     * Учитывается беззащитность атакуемой фигуры. Если фигура беззащитна, то добавляем в рейтинг стоимость атакуЮЩЕЙ
     * фигуры. Таким образом в совокупности с EXCHANGE_DIFF рейтинг будет равен стоимости срубленной фигуры.
     * <p>
     * Формула: additionalRating = pieceFrom.value
     * Формула (EXCHANGE_DIFF + ATTACK_DEFENSELESS_PIECE):
     * additionalRating = (pieceTo.value - pieceFrom.value) + pieceFrom.value = pieceTo.value
     */
    ATTACK_DEFENSELESS_PIECE(100),
    /**
     * Не надо ставить фигуру под удар
     * или
     * Надо убрать фигуру под ударом
     */
    ALLY_PIECE_RESCUE(100),
    /**
     * Шах - это тоже хорошо
     */
    CHECK(30);

    private final int factor;

    RatingParam(int factor) {
        this.factor = factor;
    }
}
