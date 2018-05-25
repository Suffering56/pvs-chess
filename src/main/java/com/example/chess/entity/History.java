package com.example.chess.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class History {

	public static History createForNextPosition(History other) {
		History result = new History();

		result.gameId = other.gameId;
		result.position = other.position + 1;
		result.pieceId = other.pieceId;
		result.rowIndex = other.rowIndex;
		result.columnIndex = other.columnIndex;

		return result;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(nullable = false)
	private Long gameId;

	@Column(nullable = false)
	private Integer position;

	@Column(nullable = false, name = "piece_id")
	private Long pieceId;

	@ManyToOne
	@JoinColumn(name = "piece_id", nullable = false, insertable = false, updatable = false)
	private Piece piece;

	@Column(nullable = false)
	private Integer rowIndex;

	@Column(nullable = false)
	private Integer columnIndex;

}
