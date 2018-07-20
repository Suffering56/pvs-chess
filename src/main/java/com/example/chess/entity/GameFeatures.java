package com.example.chess.entity;

import com.example.chess.enums.Side;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class GameFeatures {

	public GameFeatures(Game game, Side side) {
		this.game = game;
		this.side = side;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "game_id", nullable = false)
	@JsonIgnore
	private Game game;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Side side;

	private String sessionId;

	private LocalDateTime lastVisitDate;

	@ColumnDefault("true")
	@Column(nullable = false)
	private Boolean longCastlingAvailable = true;

	@ColumnDefault("true")
	@Column(nullable = false)
	private Boolean shortCastlingAvailable = true;

	//если пешка сделала длинный ход (на 2 клетки вперед) здесь храним индекс
	private Integer pawnLongMoveColumnIndex;

	@ColumnDefault("false")
	@Column(nullable = false)
	private Boolean isUnderCheck = false;
}
