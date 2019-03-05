package com.example.chess.logic.objects;

import com.example.chess.enums.RatingParam;
import com.example.chess.logic.objects.move.ExtendedMove;
import com.example.chess.logic.utils.CommonUtils;
import lombok.Getter;
import org.w3c.dom.ranges.Range;

public class Rating {

    @Getter
    private RatingParam param;
    @Getter
    private int value;

    private String reasonMove;

//    private final Map<String, Object> description = new LinkedHashMap<>();    ////TODO: возможно надо будет вернуть (1)
//    private static final String NOT_SHOW_THIS_VALUE = "NOT_SHOW_THIS_VALUE";

    private Rating() {
    }

    public static Rating.Builder builder() {
        return new Rating().new Builder();
    }

    public class Builder {

        private boolean inverted = false;

        public Builder reasonMove(ExtendedMove move) {
            reasonMove = move.toString();
            return this;
        }

        public Builder setInverted(boolean inverted) {
            this.inverted = inverted;
            return this;
        }

        public Builder var(String key, Object value) {
//            description.put(key, value);                  //TODO: возможно надо будет вернуть (1)
            return this;
        }

        public Builder note(String note) {
//            description.put(note, NOT_SHOW_THIS_VALUE);   //TODO: возможно надо будет вернуть (1)
            return this;
        }

        public Rating build(RatingParam param) {
            return build(param, 1);
        }

        public Rating build(RatingParam param, int value) {
            if (inverted) {
                value = -value;
            }

            Rating.this.value = value;
            Rating.this.param = param;
            return Rating.this;
        }
    }

    public void print(int tabsCount) {
        String str = CommonUtils.tabs(tabsCount) + param + ": " + param.getFactor() + "x" + value;
        if (reasonMove != null) {
            str += " [" + reasonMove + "]";
        }
        System.out.println(str);
    }

}
