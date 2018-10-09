package com.example.chess.service.impl;

import com.example.chess.dto.ArrangementDTO;
import com.example.chess.dto.CellDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.entity.History;
import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.exceptions.HistoryNotFoundException;
import com.example.chess.repository.GameRepository;
import com.example.chess.repository.HistoryRepository;
import com.example.chess.repository.PieceRepository;
import com.example.chess.service.GameService;
import com.example.chess.service.support.CellsMatrix;
import com.example.chess.service.support.MoveHelper;
import com.example.chess.service.support.MoveResult;
import com.example.chess.service.support.api.MoveHelperAPI;
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.example.chess.ChessConstants.BOARD_SIZE;
import static java.util.function.Function.identity;

@Log4j2
@Service
public class GameServiceImpl implements GameService {

    private final GameRepository gameRepository;
    private final HistoryRepository historyRepository;
    private final PieceRepository pieceRepository;
    private Map<Side, Map<PieceType, Piece>> piecesBySideAndTypeMap;


    public GameServiceImpl(GameRepository gameRepository, HistoryRepository historyRepository, PieceRepository pieceRepository) {
        this.gameRepository = gameRepository;
        this.historyRepository = historyRepository;
        this.pieceRepository = pieceRepository;
    }

    @PostConstruct
    private void init() {
        Iterable<Piece> pieces = pieceRepository.findAll();
        if (Iterables.isEmpty(pieces)) {
            throw new RuntimeException("pieces not found");
        }

        piecesBySideAndTypeMap = StreamSupport
                .stream(pieces.spliterator(), false)
                .collect(Collectors.groupingBy(Piece::getSide,
                        Collectors.toMap(Piece::getType, identity())));
    }

    @Override
    public ArrangementDTO createArrangementByGame(Game game, int position) throws HistoryNotFoundException {
        return createCellsMatrixByGame(game, position).generateArrangement(game.getUnderCheckSide());
    }

    @Override
    @Transactional
    public ArrangementDTO applyMove(Game game, MoveDTO move) throws HistoryNotFoundException {
        CellsMatrix prevMatrix = createCellsMatrixByGame(game, game.getPosition());

        Piece pieceFrom = prevMatrix.getCell(move.getFrom()).getPiece();
        Piece pieceTo = prevMatrix.getCell(move.getTo()).getPiece();
        Side sideFrom = pieceFrom.getSide();

        game.setPawnLongMoveColumnIndex(sideFrom, null);
        game.setUnderCheckSide(null);

        MoveResult moveResult;

        if (pieceFrom.getType() == PieceType.KING && move.isCastling()) {
            moveResult = prevMatrix.executeCastling(move);                                      //castling
        } else if (pieceFrom.getType() == PieceType.PAWN && move.isEnPassant(pieceTo)) {
            moveResult = prevMatrix.executeEnPassant(move);                                     //en passant
        } else {
            moveResult = prevMatrix.executeMove(move, getPromotionPiece(prevMatrix, move));     //simple move
        }

        switch (pieceFrom.getType()) {
            case KING:
                game.disableCasting(sideFrom);
                break;
            case ROOK:
                int rookColumnIndex = move.getFrom().getColumnIndex();
                game.disableCasting(sideFrom, rookColumnIndex);
                break;
            case PAWN:
                if (move.isLongPawnMove()) {
                    //it needs for handling of the en-passant
                    game.setPawnLongMoveColumnIndex(sideFrom, move.getFrom().getColumnIndex());
                }
                break;
        }

        CellsMatrix newMatrix = moveResult.getNewMatrix();
        List<History> newHistory = newMatrix.generateHistory(game.getId(), newMatrix.getPosition());
        game.setPosition(newMatrix.getPosition());

        Side enemySide = sideFrom.reverse();
        MoveHelperAPI moveHelper = MoveHelper.valueOf(game.toFake(), newMatrix);
        if (moveHelper.isKingUnderAttack(enemySide)) {
            /*
                Если данный ход объявил шах вражескому королю, то нужно подсветить вражеского короля на доске.
                А еще этот параметр (game.underCheckSide) используется при вычислении доступных ходов,
                т.к. если король под атакой - то далеко не каждой фигурой можно будет ходить.
             */
            game.setUnderCheckSide(enemySide);
        }

        game.getSideFeatures(sideFrom).setLastVisitDate(LocalDateTime.now());

        historyRepository.saveAll(newHistory);
        gameRepository.save(game);

        return newMatrix.generateArrangement(game.getUnderCheckSide());
    }

    @Override
    public Game findAndCheckGame(long gameId) throws GameNotFoundException {
        return gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
    }

    @Override
    public Set<PointDTO> getAvailableMoves(long gameId, PointDTO point) throws GameNotFoundException, HistoryNotFoundException {
        Game game = findAndCheckGame(gameId);
        CellsMatrix matrix = createCellsMatrixByGame(game, game.getPosition());
        MoveHelperAPI moveHelper = MoveHelper.valueOf(game.toFake(), matrix);

        return moveHelper.getFilteredAvailablePoints(point);
    }

    @Override
    public CellsMatrix createCellsMatrixByGame(Game game, int position) throws HistoryNotFoundException {
        List<History> historyList = getHistoryByGame(game, position);
        return CellsMatrix.builder(historyList, position).build();
    }

    private Piece getPromotionPiece(CellsMatrix matrix, MoveDTO move) {
        if (move.getPromotionPieceType() == null) {
            return null;
        }

        CellDTO cellFrom = matrix.getCell(move.getFrom());
        return findPieceBySideAndType(cellFrom.getSide(), move.getPromotionPieceType());
    }

    private List<History> getHistoryByGame(Game game, int position) throws HistoryNotFoundException {
        if (position == 0) {
            return createStartHistory(game.getId());
        }

        return findHistoryByGameIdAndPosition(game.getId(), position);
    }

    private List<History> findHistoryByGameIdAndPosition(long gameId, int position) throws HistoryNotFoundException {
        List<History> historyList = historyRepository.findByGameIdAndPositionOrderByRowIndexAscColumnIndexAsc(gameId, position);
        if (historyList.isEmpty()) {
            throw new HistoryNotFoundException();
        }
        return historyList;
    }

    private List<History> createStartHistory(long gameId) {
        List<History> historyList = new ArrayList<>();

        //1-8
        for (int rowIndex = 0; rowIndex < BOARD_SIZE; rowIndex++) {
            //A-H
            for (int columnIndex = 0; columnIndex < BOARD_SIZE; columnIndex++) {

                Side side = null;
                if (rowIndex == 0 || rowIndex == 1) {
                    side = Side.WHITE;
                } else if (rowIndex == 7 || rowIndex == 6) {
                    side = Side.BLACK;
                }

                PieceType pieceType = null;
                if (rowIndex == 1 || rowIndex == 6) {
                    pieceType = PieceType.PAWN;
                } else if (rowIndex == 0 || rowIndex == 7) {
                    if (columnIndex == 0 || columnIndex == 7) {
                        pieceType = PieceType.ROOK;
                    } else if (columnIndex == 1 || columnIndex == 6) {
                        pieceType = PieceType.KNIGHT;
                    } else if (columnIndex == 2 || columnIndex == 5) {
                        pieceType = PieceType.BISHOP;
                    } else if (columnIndex == 3) {
                        pieceType = PieceType.KING;
                    } else {  //columnIndex == 4
                        pieceType = PieceType.QUEEN;
                    }
                }

                if (side != null) {
                    Piece piece = findPieceBySideAndType(side, pieceType);

                    History item = History.builder()
                            .gameId(gameId)
                            .position(0)
                            .piece(piece)
                            .pieceId(piece.getId())
                            .rowIndex(rowIndex)
                            .columnIndex(columnIndex)
                            .build();

                    historyList.add(item);
                }
            }

        }

        return historyList;
    }

    private Piece findPieceBySideAndType(Side side, PieceType type) {
        return piecesBySideAndTypeMap.get(side).get(type);
    }
}
