package com.example.chess.dto;

import com.example.chess.enums.Side;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SideDTO {

    public static final String VIEWER = "VIEWER";

    String side;

    public SideDTO(Side side) {
        this.side = side.name();
    }

    @JsonIgnore
    public Side getSideAsEnum() {
        return Side.valueOf(side);
    }

    public static SideDTO createUnselected() {
        return new SideDTO();
    }
}
