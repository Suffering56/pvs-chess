package com.example.chess.logic.utils;

import com.example.chess.dto.CellDTO;
import com.example.chess.dto.PointDTO;
import com.example.chess.enums.PieceType;
import com.example.chess.logic.objects.move.ExtendedMove;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@UtilityClass
public class CommonUtils {

    private static final String GRADLE_PROPERTIES_PATH = System.getProperty("user.dir") + "/gradle.properties";

    private static Properties gradleProperties = null;

    public static String readGradleProperty(String propertyName) {
        if (gradleProperties == null) {

            try (InputStream in = new FileInputStream(GRADLE_PROPERTIES_PATH)) {
                Properties properties = new Properties();
                properties.load(in);
                gradleProperties = properties;
            } catch (Exception e) {
                log.error("error while reading gradle.properties", e);
                return null;
            }
        }

        return gradleProperties.getProperty(propertyName);
    }

    public static String generateRandomString() {
        RandomStringGenerator randomStringGenerator =
                new RandomStringGenerator.Builder()
                        .withinRange('0', 'z')
                        .filteredBy(CharacterPredicates.LETTERS, CharacterPredicates.DIGITS)
                        .build();
        return randomStringGenerator.generate(25);
    }

    public static int generateRandomInt(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    public static String convertStackTraceToString(StackTraceElement[] stackTraceElements) {
        List<String> collect = Arrays.stream(stackTraceElements)
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());
        return StringUtils.join(collect);
    }

    private static Map<Integer, String> columnNamesMap = new HashMap<Integer, String>() {{
        put(0, "h");
        put(1, "g");
        put(2, "f");
        put(3, "e");
        put(4, "d");
        put(5, "c");
        put(6, "b");
        put(7, "a");
    }};

    private static Map<PieceType, String> pieceNamesMap = new EnumMap<PieceType, String>(PieceType.class) {{
        put(PieceType.PAWN, " ");
        put(PieceType.KNIGHT, "N");
        put(PieceType.BISHOP, "B");
        put(PieceType.ROOK, "R");
        put(PieceType.QUEEN, "Q");
        put(PieceType.KING, "K");
    }};

    public static String moveToString(ExtendedMove move) {
        CellDTO from = move.getFrom();
        CellDTO to = move.getTo();

        String result = "";
        result += getPieceName(from.getPieceType(), false) + from.getPoint();
        result += "---";
        result += to.getPoint();

        if (!to.isEmpty()) {
            result = result.replace("---", "-x-");
            result += "(" + getPieceName(to.getPieceType(), true) + ")";
        }

        return result;
    }

    public static String getPieceName(PieceType pieceType, boolean printPawnName) {
        if (pieceType == null) {
            return " ";
        }
        if (pieceType == PieceType.PAWN) {
            if (printPawnName) {
                return "p";
            } else {
                return " ";
            }
        }

        return pieceNamesMap.get(pieceType);
    }

    public static String getColumnName(int columnIndex) {
        return columnNamesMap.get(columnIndex);
    }

    public static String getColumnName(PointDTO point) {
        return getColumnName(point.getColumnIndex());
    }

    public static String getColumnName(CellDTO cell) {
        return getColumnName(cell.getColumnIndex());
    }

    public static boolean fakeTrue() {
        String s = "00000";
        return s.contains("0");
    }

    public static String tabs(int count) {
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < count; i++) {
            prefix.append("\t");
        }
        return prefix.toString();
    }
}
