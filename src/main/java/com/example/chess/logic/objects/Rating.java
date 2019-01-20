package com.example.chess.logic.objects;

import com.example.chess.enums.RatingParam;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

public class Rating {

    @Getter
    private RatingParam param;
    @Getter
    private int value;

    private final Map<String, Object> description = new LinkedHashMap<>();
    private static final String NOT_SHOW_THIS_VALUE = "NOT_SHOW_THIS_VALUE";

    private Rating() {
    }

    private Rating(RatingParam param, int value) {
        this.param = param;
        this.value = value;
    }

    public static Rating.Builder builder() {
        return new Rating().new Builder();
    }

    public class Builder {

        public Builder var(String key, Object value) {
            description.put(key, value);
            return this;
        }

        public Builder note(String note) {
            description.put(note, NOT_SHOW_THIS_VALUE);
            return this;
        }

        public Rating build(RatingParam param) {
            return build(param, 1);
        }

        public Rating build(RatingParam param, int value) {
            Rating.this.param = param;
            Rating.this.value = value;
            return Rating.this;
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("\tRating[" + param + "(" + param.getFactor() + ")] = " + value + " :\r\n");
        for (String key : description.keySet()) {
            result.append("\t\t").append(key);

            Object value = description.get(key);
            if (!NOT_SHOW_THIS_VALUE.equals(value)) {
                result.append("=>").append(value);
            }

            result.append("\r\n");
        }

        return result.toString();
    }
}
