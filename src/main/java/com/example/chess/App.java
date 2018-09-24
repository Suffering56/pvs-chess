package com.example.chess;

import com.example.chess.dto.CellDTO;
import com.example.chess.entity.Game;
import com.example.chess.service.BotService;
import com.example.chess.service.MoveService;
import com.example.chess.service.impl.BotServiceImpl;
import com.example.chess.service.impl.MoveServiceImpl;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import java.util.List;
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
    public MoveService moveService() {
        return new MoveServiceImpl();
    }

    @Bean
    @Scope("prototype")
    public BotService botService() {
        return new BotServiceImpl();
    }

    @Bean
    public Function<Game, Function<List<List<CellDTO>>, MoveService>> moveServiceSupplier() {
        return game -> cellsMatrix -> {
            MoveService botService = moveService();
            botService.setGame(game);
            botService.setCellsMatrix(cellsMatrix);
            return botService;
        };
    }

    @Bean
    public Function<Game, Function<List<List<CellDTO>>, BotService>> botServiceFactory() {
        return game -> cellsMatrix -> {
            BotService botService = botService();
            botService.setGame(game);
            botService.setCellsMatrix(cellsMatrix);
            return botService;
        };
    }

}
