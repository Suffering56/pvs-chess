package com.example.chess.service.support;

import com.example.chess.ChessConstants;
import lombok.Getter;

@Getter
public enum RatingParam {
    MATERIAL_SIMPLE_MOVE(RatingParam.MATERIAL_FACTOR),      //обычный ход
    MATERIAL_SIMPLE_FREEBIE(RatingParam.MATERIAL_FACTOR),   //бот рубит фигуру нахаляву
    MATERIAL_SIMPLE_FEED(RatingParam.MATERIAL_FACTOR),      //бот отдает фигуру просто так
    MATERIAL_SIMPLE_EXCHANGE(RatingParam.MATERIAL_FACTOR),  //бот рубит фигуру, игрок рубит в ответ.

    MATERIAL_DEEP_EXCHANGE(RatingParam.MATERIAL_FACTOR),    //если рубиться до конца
    /**
     * Инвертированный материальный рейтинг.
     * Подсчитывается для ходов игрока. А то что игроку хорошо - боту плохо, и - наоборот.
     * Поэтому коэффициент отрицательный.
     */
    INVERTED_MATERIAL_FOR_PLAYER(-RatingParam.MATERIAL_FACTOR),

    AVAILABLE_MOVES_COUNT(1),
    INVERTED_AVAILABLE_MOVES_COUNT(-1),
    DEEP(1),

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
    CHECK(5),
    /**
     * А мат - еще лучше
     */
    CHECKMATE_BY_BOT(ChessConstants.CHECKMATE_VALUE),
    CHECKMATE_BY_PLAYER(-ChessConstants.CHECKMATE_VALUE),
    DEEP_2_BY_PLAYER(-1),
    DEEP_3_BY_BOT(1);


    private static final int MATERIAL_FACTOR = 100;
    private final int factor;

    RatingParam(int factor) {
        this.factor = factor;
    }
}
