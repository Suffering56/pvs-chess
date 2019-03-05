package com.example.chess.logic.utils;

@FunctionalInterface
public interface BiIntFunction<T> {

    T apply(int firstVal, int secondVal);
}
