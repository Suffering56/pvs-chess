package com.example.chess.dto;

import com.example.chess.enums.GameMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModeDTO {

    GameMode mode;
}
