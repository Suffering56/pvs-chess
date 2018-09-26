package com.example.chess.entity;

import com.example.chess.dto.CellDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@Log4j2
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

    public static History createByCell(CellDTO cell, Long gameId, Integer position) {
        History result = new History();

        result.gameId = gameId;
        result.position = position;
        result.pieceId = cell.getPiece().getId();
        result.rowIndex = cell.getRowIndex();
        result.columnIndex = cell.getColumnIndex();

        return result;
    }

    public void print() {
        log.debug("history[{},{}]: side = {}, piece = {}", rowIndex, columnIndex,
                piece.getSide(), piece.getType());
    }
}
