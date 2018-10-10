package com.example.chess;

import com.example.chess.service.support.BotMode;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Log4j2
@SpringBootApplication
public class App implements CommandLineRunner {

    public static long availablePointsFound = 0;
    public static long getUnfilteredMovesCallsCount = 0;
    public static long moveHelpersCount = 0;
    public static long movesExecuted = 0;

    public static void resetCounters() {
        availablePointsFound = 0;
        getUnfilteredMovesCallsCount = 0;
        moveHelpersCount = 0;
        movesExecuted = 0;
    }

    public static void printCounters() {
        System.out.println("moveHelpersCount = " + moveHelpersCount);
        System.out.println("movesExecuted = " + movesExecuted);
        System.out.println("availablePointsFound = " + availablePointsFound);
        System.out.println("getUnfilteredMovesCallsCount = " + getUnfilteredMovesCallsCount);
    }


    //TODO: i think, i can use @Conditional instead @Qualifier
    public static final String DEFAULT_BOT_MODE = BotMode.MEDIUM;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("APP_VERSION = " + getVersion());
        log.info("DEFAULT_BOT_MODE = " + DEFAULT_BOT_MODE);
    }

    public static String getVersion() {
        return CommonUtils.readGradleProperty("project.version");
    }
}
