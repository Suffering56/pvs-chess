package com.example.chess;

import com.example.chess.logic.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Log4j2
@SpringBootApplication
public class App implements CommandLineRunner {

    public static final int MAX_DEEP = 3;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("APP_VERSION = " + getVersion());
    }

    public static String getVersion() {
        return CommonUtils.readGradleProperty("project.version");
    }

}
