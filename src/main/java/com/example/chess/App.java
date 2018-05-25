package com.example.chess;

import com.example.chess.service.MoveService;
import com.example.chess.service.impl.MoveServiceImpl;
import com.example.chess.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import java.util.function.Supplier;

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
	public Supplier<MoveService> moveServiceSupplier() {
		return this::moveService;
	}
}
