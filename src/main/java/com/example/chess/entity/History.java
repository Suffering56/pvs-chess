package com.example.chess.entity;

import com.example.chess.dto.CellDTO;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class History {

    @Id
    @GenericGenerator(name = "history_id_seq", strategy = "sequence-identity", parameters = @org.hibernate.annotations.Parameter(name = "sequence", value = "history_id_seq"))
    @GeneratedValue(generator = "history_id_seq")
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, name = "piece_id")
    private Integer pieceId;

    @ManyToOne
    @JoinColumn(name = "piece_id", nullable = false, insertable = false, updatable = false)
    private Piece piece;

    @Column(nullable = false)
    private Integer rowIndex;

    @Column(nullable = false)
    private Integer columnIndex;

    @Transient
    public static History ofCell(CellDTO cell, Long gameId, Integer position) {
        return History.builder()
                .gameId(gameId)
                .position(position)
                .rowIndex(cell.getRowIndex())
                .columnIndex(cell.getColumnIndex())
                .piece(cell.getPiece())
                .pieceId(cell.getPiece().getId())
                .build();
    }
}
