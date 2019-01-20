package com.example.chess.entity;

import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.service.support.AbstractMove;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class History extends AbstractMove {

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
}
