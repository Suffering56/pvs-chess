package com.example.chess.enums;

import lombok.Getter;

@Getter
public enum RatingParam {
    MATERIAL_SIMPLE_MOVE(RatingParam.MATERIAL_FACTOR),      //обычный ход
    MATERIAL_SIMPLE_ATTACK(RatingParam.MATERIAL_FACTOR),   //бот рубит фигуру нахаляву
    MATERIAL_SIMPLE_FEED(RatingParam.MATERIAL_FACTOR),      //бот отдает фигуру просто так
    MATERIAL_SIMPLE_EXCHANGE(RatingParam.MATERIAL_FACTOR),  //бот рубит фигуру, игрок рубит в ответ.

    MATERIAL_DIFF(RatingParam.MATERIAL_FACTOR),             //pieceTo.value - pieceFrom.value OR zero if pointFrom is empty. по идее не может быть отрицательным

    MATERIAL_DEEP_EXCHANGE(RatingParam.MATERIAL_FACTOR),    //если рубиться до конца
    /**
     * Инвертированный материальный рейтинг.
     * Подсчитывается для ходов игрока. А то что игроку хорошо - боту плохо, и - наоборот.
     * Поэтому коэффициент отрицательный.
     */
    INVERTED_MATERIAL_FOR_PLAYER(-RatingParam.MATERIAL_FACTOR),   //TODO: WARN: negative
    DEEP_EXCHANGE_ALREADY_CALCULATED(0),

    AVAILABLE_MOVES_COUNT(1),
    INVERTED_AVAILABLE_MOVES_COUNT(-1),                     //TODO: WARN: negative

    CHECK(5);

    private static final int MATERIAL_FACTOR = 100;
    private final int factor;

    RatingParam(int factor) {
        this.factor = factor;
    }
}
