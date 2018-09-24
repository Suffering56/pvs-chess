package com.example.chess.service;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.entity.Game;
import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface MoveService {

	Set<PointDTO> getAvailableMoves(PointDTO point);

	boolean isEnemyKingUnderAttack(Side attackingSide);

	Set<PointDTO> getPiecesMoves(Side side, PieceType... pieceTypes);

	Stream<CellDTO> filteredPiecesStream(Side side, PieceType... pieceTypes);

	Stream<CellDTO> allPiecesStream();

	void setGame(Game game);

	void setCellsMatrix(List<List<CellDTO>> cellsMatrix);
}
