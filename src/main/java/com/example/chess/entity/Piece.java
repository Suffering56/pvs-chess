package com.example.chess.entity;

import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.util.List;

@Entity
@Immutable
@Getter
@ToString(exclude = "history")
public class Piece {

	@Id
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null || getClass() != o.getClass()) return false;

		Piece piece = (Piece) o;

		return new EqualsBuilder()
				.append(id, piece.id)
				.append(side, piece.side)
				.append(type, piece.type)
				.append(history, piece.history)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(id)
				.append(side)
				.append(type)
				.append(history)
				.toHashCode();
	}
}
