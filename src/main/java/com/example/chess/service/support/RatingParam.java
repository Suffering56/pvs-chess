package com.example.chess.service.support;

import lombok.Getter;

@Getter
public enum RatingParam {
    SAFE_CELL(RatingParam.MATERIAL_FACTOR),
    UNSAFE_CELL_DIFF_LVL_1(RatingParam.MATERIAL_FACTOR),
    UNSAFE_CELL_DIFF_LVL_2(RatingParam.MATERIAL_FACTOR),
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
     * Формула (EXCHANGE_DIFF + ATTACK_TO_DEFENSELESS_PLAYER_PIECE):
     * additionalRating = (pieceTo.value - pieceFrom.value) + pieceFrom.value = pieceTo.value
     */
    ATTACK_TO_DEFENSELESS_PLAYER_PIECE(100),
    /**
     * Надо убрать фигуру под ударом
     * TODO: или же срубить атакующую
     */
    SAVE_BOT_PIECE(100),
    /**
     * Не надо ставить фигуру под удар
     */
    USELESS_VICTIM(-100),
    /**
     * Шах - это тоже хорошо
     */
    CHECK(30),
    /**
     * А мат - еще лучше
     */
    CHECKMATE(100000);

    //pawn promotion
    //fork (вилка)
    //pin (связка)
    //sacrifice
    //hidden check

    private static final int MATERIAL_FACTOR = 100;
    private final int factor;

    RatingParam(int factor) {
        this.factor = factor;
    }
}
