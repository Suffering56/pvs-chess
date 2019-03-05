package com.example.chess.dto;

import com.example.chess.enums.Side;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArrangementDTO {

    int position;
    CellDTO[][] cellsMatrix;
    Side underCheckSide;
}
