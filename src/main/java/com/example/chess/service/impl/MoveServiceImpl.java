package com.example.chess.service.impl;

import com.example.chess.aspects.Profile;
import com.example.chess.dto.PointDTO;
import com.example.chess.dto.MoveDTO;
import com.example.chess.dto.CellDTO;
import com.example.chess.entity.Game;
import com.example.chess.entity.Piece;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.MoveService;
import com.example.chess.utils.ChessUtils;
import com.example.chess.utils.MoveResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.chess.ChessConstants.BOARD_SIZE;

@Service //prototype
@Log4j2
public class MoveServiceImpl implements MoveService {

	//final
	private Game game;
	private List<List<CellDTO>> cellsMatrix;
	private Side originalEnemySide;
	private PointDTO originalKingPoint;
	private Piece originalPiece;

	//changed recursive
	private CellDTO activeCell;
	private Side activeSelfSide;
	private Side activeEnemySide;

	@Override
	public boolean isEnemyKingUnderAttack(Side attackingSide) {
		PointDTO enemyKingPoint = findKingPoint(attackingSide.reverse());
		Set<PointDTO> enemyMoves = getPiecesMoves(attackingSide, PieceType.pawn, PieceType.knight, PieceType.bishop, PieceType.rook, PieceType.queen);
		return enemyMoves.contains(enemyKingPoint);
	}

	@Override
	@Profile
	public Set<PointDTO> getAvailableMoves(PointDTO pointFrom) {
		CellDTO originalCell = ChessUtils.getCell(cellsMatrix, pointFrom);
		this.originalPiece = originalCell.getPiece();
		this.originalEnemySide = getEnemySide(originalCell);
		this.originalKingPoint = findKingPoint(originalCell.getPieceSide());

		Set<PointDTO> moves = getAvailableMovesForCell(originalCell);

		moves = moves.stream()
				.filter(isNotAttackingBySimpleEnemyPieces(originalCell)
						.and(isNotAttackingByEnemyPawns())
				)
				.collect(Collectors.toSet());

		if (originalCell.getPieceType() == PieceType.king) {
			Set<PointDTO> unavailableCastlingMoves = getUnavailableCastlingPoints(pointFrom, moves);
			moves.removeAll(unavailableCastlingMoves);            //запрещаем рокировку если король пересекает битое поле
		}

		return moves;
	}

	private Set<PointDTO> getAvailableMovesForCell(CellDTO cell) {
		this.activeCell = cell;
		this.activeSelfSide = cell.getPieceSide();
		this.activeEnemySide = getEnemySide(cell);

		Set<PointDTO> moves;

		switch (cell.getPieceType()) {
			case pawn: {
				moves = getMovesForPawn();
				break;
			}
			case knight: {
				moves = getMovesForKnight();
				break;
			}
			case bishop: {
				moves = getMovesForBishop();
				break;
			}
			case rook: {
				moves = getMovesForRook();
				break;
			}
			case queen: {
				moves = getMovesForQueen();
				break;
			}
			case king: {
				moves = getMovesForKing();
				break;
			}
			default:
				moves = new HashSet<>();
		}
		return moves;
	}

	private Set<PointDTO> getMovesForPawn() {
		Set<PointDTO> moves = new HashSet<>();

		int activeRow = activeCell.getRowIndex();
		int activeColumn = activeCell.getColumnIndex();

		int vector = 1;
		if (activeCell.getPieceSide() == Side.black) {
			vector = -1;
		}

		boolean isFirstMove = false;
		if (activeRow == 1 || activeRow == 6) {
			isFirstMove = true;
		}

		@SuppressWarnings("PointlessArithmeticExpression")
		CellDTO cell = getCell(activeRow + 1 * vector, activeColumn);
		boolean isAdded = addPawnMove(moves, cell, false);

		if (isFirstMove && isAdded) {
			cell = getCell(activeRow + 2 * vector, activeColumn);
			addPawnMove(moves, cell, false);
		}

		//attack
		cell = getCell(activeRow + vector, activeColumn + 1);
		addPawnMove(moves, cell, true);

		cell = getCell(activeRow + vector, activeColumn - 1);
		addPawnMove(moves, cell, true);

		Integer enemyLongMoveColumnIndex = game.getPawnLongMoveColumnIndex(activeEnemySide);
		if (enemyLongMoveColumnIndex != null) {    //противник только что сделал длинный ход пешкой

			if (Math.abs(enemyLongMoveColumnIndex - activeCell.getColumnIndex()) == 1) {    //и эта пешка рядом с выделенной (слева или справа)

				if (activeCell.getRowIndex() == 3 || activeCell.getRowIndex() == 4) {        //и это творится на нужной горизонтали
					//значит можно делать взятие на проходе
					//проверять ничего не нужно, эта ячейка 100% пуста (не могла же пешка перепрыгнуть фигуру)
					moves.add(new PointDTO(activeCell.getRowIndex() + getPawnMoveVector(activeSelfSide), enemyLongMoveColumnIndex));
				}
			}
		}

		return moves;
	}

	private int getPawnMoveVector(Side side) {
		if (side == Side.white) {
			return 1;
		} else {
			return -1;
		}
	}

	private Set<PointDTO> getMovesForKnight() {
		Set<PointDTO> moves = new HashSet<>();

		checkAndAddKnightMove(moves, 1, 2);
		checkAndAddKnightMove(moves, 2, 1);
		checkAndAddKnightMove(moves, 1, -2);
		checkAndAddKnightMove(moves, 2, -1);
		checkAndAddKnightMove(moves, -1, 2);
		checkAndAddKnightMove(moves, -2, 1);
		checkAndAddKnightMove(moves, -1, -2);
		checkAndAddKnightMove(moves, -2, -1);

		return moves;
	}

	private void checkAndAddKnightMove(Set<PointDTO> moves, int rowOffset, int columnOffset) {
		CellDTO cell = getCell(activeCell.getRowIndex() + rowOffset, activeCell.getColumnIndex() + columnOffset);
		if (cell != null && cell.getPieceSide() != activeSelfSide) {
			moves.add(cell.generatePoint());
		}
	}

	private Set<PointDTO> getMovesForBishop() {
		Set<PointDTO> moves = new HashSet<>();

		addAvailableMovesForRay(moves, 1, 1);
		addAvailableMovesForRay(moves, -1, 1);
		addAvailableMovesForRay(moves, 1, -1);
		addAvailableMovesForRay(moves, -1, -1);

		return moves;
	}

	private Set<PointDTO> getMovesForRook() {
		Set<PointDTO> moves = new HashSet<>();

		addAvailableMovesForRay(moves, 1, 0);
		addAvailableMovesForRay(moves, -1, 0);
		addAvailableMovesForRay(moves, 0, 1);
		addAvailableMovesForRay(moves, 0, -1);

		return moves;
	}

	private Set<PointDTO> getMovesForQueen() {
		Set<PointDTO> moves = new HashSet<>();

		moves.addAll(getMovesForRook());
		moves.addAll(getMovesForBishop());

		return moves;
	}

	private Set<PointDTO> getMovesForKing() {
		Set<PointDTO> moves = new HashSet<>();

		addAvailableMovesForRay(moves, 1, 0, 1);
		addAvailableMovesForRay(moves, -1, 0, 1);
		addAvailableMovesForRay(moves, 0, 1, 1);
		addAvailableMovesForRay(moves, 0, -1, 1);
		addAvailableMovesForRay(moves, 1, 1, 1);
		addAvailableMovesForRay(moves, -1, 1, 1);
		addAvailableMovesForRay(moves, 1, -1, 1);
		addAvailableMovesForRay(moves, -1, -1, 1);

		if (game.isShortCastlingAvailable(activeSelfSide)) {
			if (isEmptyCellsByActiveRow(1, 2)) {
				addMove(moves, getCell(activeCell.getRowIndex(), activeCell.getColumnIndex() - 2));
			}
		}
		if (game.isLongCastlingAvailable(activeSelfSide)) {
			if (isEmptyCellsByActiveRow(4, 5, 6)) {
				addMove(moves, getCell(activeCell.getRowIndex(), activeCell.getColumnIndex() + 2));
			}
		}

		return moves.stream()
				//фильровать здесь!!!
				//если это делать там, где происходят остальные проверки на шах - попадем в бесконечную рекурсию
				.filter(isNotAttackingByEnemyKing())
				.collect(Collectors.toSet());
	}

	/**
	 * Проверяет не встали ли мы под шах от простой фигуры (коня/слона/ладьи/ферзя).
	 * Так же проверяет, не передвинули ли мы фигуру, защищающую короля от шаха вражеской простой фигуры
	 */
	private Predicate<PointDTO> isNotAttackingBySimpleEnemyPieces(CellDTO originalCell) {
		Piece originalPiece = originalCell.getPiece();

		return pointTo -> {
			//имитируем ход
			MoveResult moveResult = ChessUtils.executeMove(cellsMatrix, new MoveDTO(originalCell.generatePoint(), pointTo));

			//для всех дальнобойных фигур собираем все доступные ходы врага на следующий ход
			Set<PointDTO> rayPieceMoves = getPiecesMoves(originalEnemySide, PieceType.knight, PieceType.bishop, PieceType.rook, PieceType.queen);

			//и проверяем, что мы не поставили нашего короля под атаку
			boolean isCanAttack;
			if (originalPiece.getType() == PieceType.king) {
				//если мы пошли королем, то не встал ли он под шах
				isCanAttack = rayPieceMoves.contains(pointTo);
			} else {
				//если другой фигурой - а не открылся ли шах
				isCanAttack = rayPieceMoves.contains(originalKingPoint);
			}
			//откатываем ход
			moveResult.rollbackMove();

			return !isCanAttack;
		};
	}

	/**
	 * Проверяет не встали ли мы королем под шах пешки противника
	 */
	private Predicate<? super PointDTO> isNotAttackingByEnemyPawns() {

		if (originalPiece.getType() == PieceType.king) {				//если ходим королем, то не даем ему пойти под шах от пешки
			Set<PointDTO> enemyPawnAttackMoves = getPawnAttackMoves(originalEnemySide, null);
			return pointTo -> !enemyPawnAttackMoves.contains(pointTo);

		} else {														//если другой фигурой
			if (game.getUnderCheckSide() == originalPiece.getSide()) {	//и наш король находится под шахом от вражеской пешки
																		//не даем фигурам ходить, кроме случая если она рубит пешку объявившую шах
				return pointTo -> !getPawnAttackMoves(originalEnemySide, pointTo).contains(originalKingPoint);

			} else {													//если король не под шахом то шаха от вражеской пешки быть не может
				return pointTo -> true;									//ведь шаха не было, а ходим мы не королем
			}
		}
	}

	/**
	 * Проверяет не встали ли мы королем под шах вражеского короля.
	 */
	private Predicate<PointDTO> isNotAttackingByEnemyKing() {
		PointDTO enemyKingPoint = findKingPoint(originalEnemySide);
		return enemyKingPoint::isNotBorderedBy;  //или может подошли вплотную к вражескому королю?
	}

	private Set<PointDTO> getUnavailableCastlingPoints(PointDTO pointFrom, Set<PointDTO> moves) {
		return moves.stream()
				.filter(pointFrom::isNotBorderedBy)            //если pointTo не граничит с позицией короля значит это рокировка
				.filter(castlingPoint -> {
					int castlingVector = (castlingPoint.getColumnIndex() - pointFrom.getColumnIndex()) / 2;             //определяем вектор рокировки
					int crossColumnIndex = pointFrom.getColumnIndex() + castlingVector;                                 //прибавляем к позиции короля
					PointDTO crossPoint = new PointDTO(pointFrom.getRowIndex(), crossColumnIndex);                      //получаем пересекаемое при рокировке поле

					//проверяем есть ли оно среди доступных ходов (если нет -> значит оно битое -> значит рокировка в эту сторону запрещена)
					return !moves.contains(crossPoint);
				}).collect(Collectors.toSet());
	}

	private Set<PointDTO> getPawnAttackMoves(Side side, PointDTO excludePoint) {
		Set<PointDTO> enemyPawnCoords = findPiecesCoords(side, PieceType.pawn);
		enemyPawnCoords.remove(excludePoint);

		int vector = getPawnMoveVector(side);

		Set<PointDTO> pawnAttackMoves = new HashSet<>();
		enemyPawnCoords.forEach(point -> {
			pawnAttackMoves.add(new PointDTO(point.getRowIndex() + vector, point.getColumnIndex() + 1));
			pawnAttackMoves.add(new PointDTO(point.getRowIndex() + vector, point.getColumnIndex() - 1));
		});

		return pawnAttackMoves;
	}

	private Set<PointDTO> getPiecesMoves(Side side, PieceType... pieceTypes) {
		return filteredPiecesStream(side, pieceTypes)
				.map(this::getAvailableMovesForCell)
				.flatMap(Set::stream)
				.collect(Collectors.toSet());
	}

	private Set<PointDTO> findPiecesCoords(Side side, PieceType... pieceTypes) {
		return filteredPiecesStream(side, pieceTypes)
				.map(CellDTO::generatePoint)
				.collect(Collectors.toSet());
	}

	private Stream<CellDTO> filteredPiecesStream(Side side, PieceType... pieceTypes) {
		return allPiecesStream()
				.filter(containsPieces(side, pieceTypes));
	}

	private Stream<CellDTO> allPiecesStream() {
		return cellsMatrix.stream()
				.flatMap(List::stream);
	}

	private Predicate<CellDTO> containsPieces(Side side, PieceType[] pieceTypes) {
		return cell -> cell.getPieceSide() == side && Arrays.stream(pieceTypes).anyMatch(type -> type == cell.getPieceType());
	}

	private boolean isEmptyCellsByActiveRow(int... columnIndexes) {
		for (int columnIndex : columnIndexes) {
			if (!isEmptyCell(activeCell.getRowIndex(), columnIndex)) {
				return false;
			}
		}

		return true;
	}

	private boolean isEmptyCell(int rowIndex, int columnIndex) {
		CellDTO cell = ChessUtils.getCell(cellsMatrix, rowIndex, columnIndex);
		return cell.getPiece() == null;
	}

	private void addAvailableMovesForRay(Set<PointDTO> moves, int rowVector, int columnVector) {
		addAvailableMovesForRay(moves, rowVector, columnVector, 7);
	}

	private void addAvailableMovesForRay(Set<PointDTO> moves, int rowVector, int columnVector, int rayLength) {
		for (int i = 1; i < rayLength + 1; i++) {
			CellDTO cell = getCell(activeCell.getRowIndex() + rowVector * i, activeCell.getColumnIndex() + columnVector * i);
			if (!addMove(moves, cell)) {
				break;
			}
		}
	}

	private boolean addMove(Set<PointDTO> moves, CellDTO cell) {
		if (cell == null || cell.getPieceSide() == activeSelfSide) {
			//IndexOutOfBounds
			return false;
		}

		moves.add(cell.generatePoint());
		return cell.getPieceSide() != activeEnemySide;
	}

	private boolean addPawnMove(Set<PointDTO> moves, CellDTO cell, boolean isAttack) {
		if (cell == null || cell.getPieceSide() == activeSelfSide) {
			//IndexOutOfBounds
			return false;
		}

		if (isAttack) {
			if (cell.getPieceSide() == activeEnemySide) {
				moves.add(cell.generatePoint());
			}
			return false;
		} else {
			if (cell.getPieceSide() == activeEnemySide) {
				return false;
			} else {
				moves.add(cell.generatePoint());
				return true;
			}
		}
	}

	private Side getEnemySide(CellDTO cell) {
		if (cell.getPieceSide() == Side.black) {
			return Side.white;
		} else {
			return Side.black;
		}
	}

	private CellDTO getCell(int rowIndex, int columnIndex) {
		if (rowIndex >= 0 && rowIndex < BOARD_SIZE && columnIndex >= 0 && columnIndex < BOARD_SIZE) {
			return cellsMatrix.get(rowIndex).get(columnIndex);
		} else {
			return null;
		}
	}

	private PointDTO findKingPoint(Side side) {
		Objects.requireNonNull(side, "side is null");

		return filteredPiecesStream(side, PieceType.king)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("king not found on board"))
				.generatePoint();
	}

	@Override
	public void setGame(Game game) {
		this.game = game;
	}

	@Override
	public void setCellsMatrix(List<List<CellDTO>> cellsMatrix) {
		this.cellsMatrix = cellsMatrix;
	}
}
