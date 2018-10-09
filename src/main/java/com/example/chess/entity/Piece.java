package com.example.chess.entity;

import com.example.chess.enums.PieceType;
import com.example.chess.enums.Side;
import com.example.chess.service.support.Immutable;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;

@Entity
@Getter
@ToString
@org.hibernate.annotations.Immutable
@SuppressWarnings("unused")
public final class Piece implements Immutable {

    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Side side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PieceType type;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Piece piece = (Piece) o;
        return new EqualsBuilder()
                .append(id, piece.id)
                .append(side, piece.side)
                .append(type, piece.type)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(side)
                .append(type)
                .toHashCode();
    }
}
