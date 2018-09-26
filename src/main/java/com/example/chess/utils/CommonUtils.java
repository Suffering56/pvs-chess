package com.example.chess.utils;

import com.example.chess.exceptions.GameNotFoundException;
import com.example.chess.exceptions.HistoryNotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

@Log4j2
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

    public static void executeInSecondaryThread(Executor executor) {
        new Thread(() -> {
            try {
                executor.execute();
            } catch (InterruptedException | GameNotFoundException | HistoryNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }).start();
    }


    public interface Executor {
        void execute() throws InterruptedException, GameNotFoundException, HistoryNotFoundException;
    }
}
