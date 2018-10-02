package com.example.chess.service.support.api;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.Side;
import com.example.chess.service.support.ExtendedCell;
import com.example.chess.service.support.ExtendedMove;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface MoveHelperAPI {

    /**
     * Находит все доступные ходы для фигуры, стоящей в точке pointFrom.
     * Это уже ОТФИЛЬТРОВАННЫЕ ходы, т.е. они не могут нарушить целостность партии
     */
    Set<PointDTO> getFilteredAvailableMoves(PointDTO pointFrom);

    Set<PointDTO> getFilteredAvailableMoves(CellDTO moveableCell);

    /**
     * Проверяет находится ли под шахом король в данный момент! Уже!!!
     * Работает это так:
     * Сначала находятся НЕОТФИЛЬТРОВАННЫЕ ходы противника и если хотя бы у одного из них конечная точка хода
     * совпадает с местоположением нашего короля, значит фигура его может срубить и... значит ход который был УЖЕ сделан
     * делать на самом-то деле нельзя.
     * А неотфильтрованные потому, что даже если противник рубя нашего короля ставит под удар своего короля,
     * то он(противник) все-равно прав, т.к. наш король падает первым.
     *
     * @param kingSide - сторона, чьего короля проверяем на стояние под шахом
     */
    boolean isKingUnderAttack(Side kingSide);

    Stream<ExtendedMove> getExtendedMovesStream(Side side);
}
