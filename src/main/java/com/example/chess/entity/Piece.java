package com.example.chess.entity;

import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
public class Piece {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Side side;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PieceType type;

	@OneToMany(mappedBy = "piece", fetch = FetchType.LAZY)
	@JsonIgnore
	private List<History> history;
}
