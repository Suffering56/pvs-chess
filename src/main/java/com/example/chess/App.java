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

    //TODO: i think, i can use @Conditional instead @Qualifier
    public static final String DEFAULT_BOT_MODE = BotMode.EASY;

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
