package com.example.chess.entity;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.logic.objects.CellsMatrix;
import com.example.chess.logic.objects.move.ExtendedMove;
import com.example.chess.logic.objects.move.Move;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class History implements Move {

    @Id
    @GenericGenerator(name = "history_id_seq", strategy = "sequence-identity", parameters = @org.hibernate.annotations.Parameter(name = "sequence", value = "history_id_seq"))
    @GeneratedValue(generator = "history_id_seq")
    private long id;

    @Column(nullable = false)
    private long gameId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private int rowIndexFrom;

    @Column(nullable = false)
    private int columnIndexFrom;

    @Column(nullable = false)
    private int rowIndexTo;

    @Column(nullable = false)
    private int columnIndexTo;

    @Column
    private PieceType pieceFromPawn;

    @Column
    private String description;

    @Override
    @Transient
    public PointDTO getPointFrom() {
        return PointDTO.valueOf(rowIndexFrom, columnIndexFrom);
    }

    @Override
    @Transient
    public PointDTO getPointTo() {
        return PointDTO.valueOf(rowIndexTo, columnIndexTo);
    }

    @Transient
    public ExtendedMove toExtendedMove(CellsMatrix matrix) {
        CellDTO from = matrix.getCell(getPointFrom());
        CellDTO to = matrix.getCell(getPointTo());
        return new ExtendedMove(from, to);
    }

    @Transient
    public String toReadableString() {
        return getPointFrom() + "-" + getPointTo();
    }

    @Transient
    public String getFormattedPosition() {
        return String.format("%02d", position);
    }
}
