package com.example.chess.service.impl.bot;

import com.example.chess.App;
import com.example.chess.enums.RatingParam;
import com.example.chess.exceptions.CheckmateException;
import com.example.chess.logic.MoveHelper;
import com.example.chess.logic.objects.Rating;
import com.example.chess.logic.objects.game.GameContext;
import com.example.chess.logic.objects.move.ExtendedMove;
import com.google.common.base.Preconditions;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import static com.example.chess.service.impl.bot.MaterialRatingCalculator.getInvertedMaterialRating;
import static com.example.chess.service.impl.bot.MaterialRatingCalculator.getMaterialRating;


@Service
@Log4j2
public class BotServiceImplMedium extends AbstractBotService {

    @Override
    protected void calculateRating(GameContext gameContext) throws CheckmateException {
        Preconditions.checkState(!gameContext.isRoot());
        Preconditions.checkState(gameContext.getDeep() <= App.MAX_DEEP);

        ExtendedMove analyzedMove = gameContext.getLastMove();

        if (gameContext.getDeep() == App.MAX_DEEP) {
            analyzedMove.updateRating(getMaterialRating(gameContext));
            analyzedMove.updateRating(getInvertedMaterialRating(gameContext));
        }
        else {
            //вот это тоже оч важная вещь, только от k нужно будет избавиться
            int k = gameContext.botLast() ? 1 : -1;
            analyzedMove.updateRating(
                    Rating.builder().build(RatingParam.MATERIAL_DIFF,
                            k * gameContext.getLastMove().getValueTo(0))
            );
        }



//        analyzedMove
//                .updateRating(getCheckRating(gameContext))
//                .updateRating(getMovesCountRating(gameContext, false));
    }

    private Rating getCheckRating(GameContext gameContext) {
        if (gameContext.hasChildren()) {
            long count = gameContext.childrenStream(gameContext.getMatrix().getKingPoint(gameContext.lastMoveSide())).count();
            if (count > 0) {
                return Rating.builder().build(RatingParam.CHECK);
            }
        }

        return Rating.builder().build(RatingParam.CHECK, 0);
    }

    /**
     * Считает количество ходов expectedSide до хода бота и после.
     * Разницу записывает в рейтинг. diff = movesAfter.size - movesBefore.size
     * expectedSide - может быть как ботом так и игроком.
     * <p>
     * Если expectedSide == botSide:
     * - боту выгодно, если сделанный ход дает ЕМУ БОЛЬШЕ пространства (возможностей) на доске.
     * - значит боту выгодно когда diff > 0; (значит что текущий ход позволяет боту на следующий ход получить бОльшую вариацию ходов)
     * <p>
     * Если expectedSide == playerSide:
     * - боту выгодно, если сделанный ход дает ИГРОКУ МЕНЬШЕ пространства.
     * - значит боту выгодно когда diff < 0;
     * <p>
     * FIXME: в этом рейтинге есть косяки:
     * 1) Он заставляет ферзя рано ходить. Т.к. ферзь в центре - приведет к максимальному коэффициенту.
     * 2) Он пытается убрать пешки с вертикалей A,H чтобы как можно раньше вывести ладьи (тем самым исключая собственную рокировку)
     * 3) Можно попасть под троекратное повторение одной и той же позиции (было такое, правда до внедрения INVERTED_AVAILABLE_MOVES_COUNT)
     */
    private Rating getMovesCountRating(GameContext context, boolean isInverted) {
        int movesBefore = (int) (context.neighboursStream()
                //.filter(neighbor -> neighbor.getLastMove().getPieceFrom() != PieceType.KING)
                .count() + 1);

        int movesAfter = (int) MoveHelper.valueOf(context)
                .getStandardMovesStream(context.nextTurnSide().reverse())
                //.filter(move -> move.getPieceFrom() != PieceType.KING)
                .count();

        Rating.Builder builder = Rating.builder()
                .var("movesBefore", movesBefore)
                .var("movesAfter", movesAfter);

        if (isInverted) {
            return builder.build(RatingParam.INVERTED_AVAILABLE_MOVES_COUNT, movesAfter - movesBefore);
        } else {
            return builder.build(RatingParam.AVAILABLE_MOVES_COUNT, movesAfter - movesBefore);
        }
    }
}


