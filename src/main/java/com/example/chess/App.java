package com.example.chess;

import com.example.chess.dto.CellsMatrix;
import com.example.chess.entity.Game;
import com.example.chess.service.BotService;
import com.example.chess.service.impl.BotServiceImpl;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import java.util.function.Function;

@Log4j2
@SpringBootApplication
public class App implements CommandLineRunner {

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

    @Bean
    @Scope("prototype")
    public BotService botService() {
        return new BotServiceImpl();
    }

    @Bean
    public Function<Game, Function<CellsMatrix, BotService>> botServiceFactory() {
        return game -> cellsMatrix -> {
            BotService botService = botService();
            botService.setGame(game);
            botService.setCellsMatrix(cellsMatrix);
            return botService;
        };
    }

}
