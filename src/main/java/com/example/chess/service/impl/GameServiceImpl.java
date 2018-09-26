package com.example.chess.service.impl;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.*;
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
import com.example.chess.utils.MoveResult;
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.example.chess.ChessConstants.*;
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
        return getCellsMatrixByGame(game, position).generateArrangement(game.getUnderCheckSide());
    }

    @Override
    @Transactional
    @Profile
    public ArrangementDTO applyMove(Game game, MoveDTO move) throws HistoryNotFoundException {
        Long gameId = game.getId();
        int newPosition = game.getPosition() + 1;

        List<History> beforeMoveHistory;
        if (game.getPosition() == 0) {
            beforeMoveHistory = createStartHistory(gameId);
        } else {
            beforeMoveHistory = findHistoryByGameIdAndPosition(gameId, game.getPosition());
        }

        CellsMatrix matrix = CellsMatrix.createByHistory(beforeMoveHistory, newPosition);

        //move piece
        Piece transformationPiece = getPawnTransformationPiece(matrix, move);
        MoveResult moveResult = matrix.executeMove(move, transformationPiece);
        Piece pieceFrom = moveResult.getPieceFrom();
        Side sideFrom = pieceFrom.getSide();

        game.setPawnLongMoveColumnIndex(sideFrom, null);
        game.setUnderCheckSide(null);

        if (pieceFrom.getType() == PieceType.KING) {
            //do castling (only the ROOK moves)
            checkAndExecuteCastling(matrix, move);

            game.disableShortCasting(sideFrom);
            game.disableLongCasting(sideFrom);

        } else if (pieceFrom.getType() == PieceType.ROOK) {

            if (game.isShortCastlingAvailable(sideFrom) && move.getFrom().getColumnIndex() == ROOK_SHORT_COLUMN_INDEX) {
                game.disableShortCasting(sideFrom);

            } else if (game.isLongCastlingAvailable(sideFrom) && move.getFrom().getColumnIndex() == ROOK_LONG_COLUMN_INDEX) {
                game.disableLongCasting(sideFrom);
            }
        } else if (pieceFrom.getType() == PieceType.PAWN) {
            int diff = move.getFrom().getRowIndex() - move.getTo().getRowIndex();

            if (Math.abs(diff) == 2) {//is long move
                game.setPawnLongMoveColumnIndex(sideFrom, move.getFrom().getColumnIndex());
            }

            if (!Objects.equals(move.getFrom().getColumnIndex(), move.getTo().getColumnIndex())) {

                if (moveResult.getPieceTo() == null) {
                    //так это взятие на проходе (не могла же пешка покинуть свою вертикаль и при этом ничего не срубив)

                    //рубим пешку
                    CellDTO enemyPawnCell = matrix.getCell(move.getFrom().getRowIndex(), move.getTo().getColumnIndex());
                    enemyPawnCell.setPiece(null);
                }
            }
        }

        List<History> afterMoveHistory = matrix.generateHistory(gameId, newPosition);
        game.setPosition(newPosition);

        MoveHelper moveHelper = new MoveHelper(game, matrix);
        boolean isEnemyKingUnderAttack = moveHelper.isEnemyKingUnderAttack(sideFrom);
        if (isEnemyKingUnderAttack) {
            game.setUnderCheckSide(sideFrom.reverse());
        }

        game.getSideFeatures(sideFrom).setLastVisitDate(LocalDateTime.now());

        historyRepository.saveAll(afterMoveHistory);
        gameRepository.save(game);

        return matrix.generateArrangement(game.getUnderCheckSide());
    }

    @Override
    public Game findAndCheckGame(long gameId) throws GameNotFoundException {
        return gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
    }

    @Override
    public Set<PointDTO> getAvailableMoves(long gameId, PointDTO point) throws GameNotFoundException, HistoryNotFoundException {
        Game game = findAndCheckGame(gameId);
        CellsMatrix matrix = getCellsMatrixByGame(game, game.getPosition());

        MoveHelper moveHelper = new MoveHelper(game, matrix);
        return moveHelper.getAvailableMoves(point);
    }

    private void checkAndExecuteCastling(CellsMatrix matrix, MoveDTO move) {
        int diff = move.getFrom().getColumnIndex() - move.getTo().getColumnIndex();

        if (Math.abs(diff) == 2) {    //is castling
            Integer kingFromColumnIndex = move.getFrom().getColumnIndex();

            //short
            PointDTO rookFrom = new PointDTO(move.getFrom().getRowIndex(), ROOK_SHORT_COLUMN_INDEX);
            PointDTO rookTo = new PointDTO(move.getFrom().getRowIndex(), kingFromColumnIndex - 1);

            //long
            if (diff < 0) {
                rookFrom.setColumnIndex(ROOK_LONG_COLUMN_INDEX);
                rookTo.setColumnIndex(kingFromColumnIndex + 1);
            }

            //move ROOK
            matrix.executeMove(new MoveDTO(rookFrom, rookTo));
        }
    }

    private Piece getPawnTransformationPiece(CellsMatrix cellsMatrix, MoveDTO move) {
        if (move.getPieceType() == null) {
            return null;
        }

        CellDTO cellFrom = cellsMatrix.getCell(move.getFrom());
        return findPieceBySideAndType(cellFrom.getPieceSide(), move.getPieceType());
    }

    @Override
    public CellsMatrix getCellsMatrixByGame(Game game, int position) throws HistoryNotFoundException {
        List<History> historyList;

        if (position == 0) {
            historyList = createStartHistory(game.getId());
        } else {
            historyList = findHistoryByGameIdAndPosition(game.getId(), position);
        }

        return CellsMatrix.createByHistory(historyList, position);
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

                    History item = new History();
                    item.setGameId(gameId);
                    item.setPosition(0);
                    item.setPieceId(piece.getId());
                    item.setPiece(piece);
                    item.setRowIndex(rowIndex);
                    item.setColumnIndex(columnIndex);

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
