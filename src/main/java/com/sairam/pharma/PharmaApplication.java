package com.sairam.pharma;

// ================================================================
// PharmaApplication.java  —  THE ENTRY POINT
//
// This is where Spring Boot starts.
// When you run "mvn spring-boot:run" or press Run in IntelliJ,
// Java executes the main() method here.
//
// @SpringBootApplication is actually 3 annotations in one:
//   @Configuration     = this class can define beans
//   @EnableAutoConfiguration = Spring auto-configures based on
//                              the dependencies in pom.xml
//                              (sees MySQL driver → sets up DB connection)
//   @ComponentScan     = scan this package and sub-packages for
//                        @Service, @Repository, @Controller etc.
// ================================================================

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
