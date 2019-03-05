package com.example.chess.logic.objects;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
public class ExtendedCell {

    private CellDTO cellFrom;
    private final Set<PointDTO> availablePoints;

    public ExtendedCell(CellDTO cellFrom) {
        this.cellFrom = cellFrom;
        this.availablePoints = new HashSet<>();
    }

    public ExtendedCell(CellDTO cellFrom, Set<PointDTO> availablePoints) {
        this.cellFrom = cellFrom;
        this.availablePoints = availablePoints;
    }

    public int count() {
        return availablePoints.size();
    }

    public boolean isEmpty() {
        return availablePoints.isEmpty();
    }
}
