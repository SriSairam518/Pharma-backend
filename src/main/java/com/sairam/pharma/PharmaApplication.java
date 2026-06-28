package com.sairam.pharma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PharmaApplication {
	public static void main(String[] args) {

		SpringApplication.run(PharmaApplication.class, args);
		System.out.println("""
            ╔═══════════════════════════════════════╗
            ║   Pharma Backend started successfully ║
            ║   API running at: localhost:8081      ║
            ╚═══════════════════════════════════════╝
            """);
	}

}
