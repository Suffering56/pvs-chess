package com.example.chess.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class Game {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ColumnDefault("0")
	@Column(nullable = false)
	private Integer position = 0;

	private String whiteSessionId;

	private String blackSessionId;

	@ColumnDefault("true")
	@Column(nullable = false)
	private Boolean isWhiteLongCastlingAvailable = true;

	@ColumnDefault("true")
	@Column(nullable = false)
	private Boolean isWhiteShortCastlingAvailable = true;

	@ColumnDefault("true")
	@Column(nullable = false)
	private Boolean isBlackLongCastlingAvailable = true;

	@ColumnDefault("true")
	@Column(nullable = false)
	private Boolean isBlackShortCastlingAvailable = true;
}
