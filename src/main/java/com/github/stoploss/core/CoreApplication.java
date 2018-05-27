package com.github.stoploss.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class CoreApplication {

	public static void main(String[] args) {
		ApiContextInitializer.init();
		SpringApplication.run(CoreApplication.class, args);
	}
}
