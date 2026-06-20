package com.sairam.pharma.config;

// ================================================================
// CorsConfig.java  —  CROSS ORIGIN RESOURCE SHARING
//
// WHAT IS CORS AND WHY DO WE NEED IT?
// By default, browsers BLOCK requests between different origins.
// "Origin" = protocol + domain + port.
//
// Your React app runs on:  http://localhost:5173
// Your Spring Boot runs on: http://localhost:8080
//
// These are DIFFERENT origins (different ports).
// Without CORS config, the browser will block every API call
// from React to Spring Boot with an error like:
//   "Access to fetch blocked by CORS policy"
//
// This config tells Spring:
// "It's okay to accept requests from http://localhost:5173"
// ================================================================

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    // Set FRONTEND_URL environment variable on Render to your Netlify URL
    // e.g. https://pharma-shop.netlify.app
    // Falls back to localhost for local development
    @Value("${frontend.url}")
    private String frontendUrl;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:5173",    // local React dev
                "http://localhost:3000",    // alternative local
                frontendUrl                 // production Netlify URL (from env var)
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", source.getCorsConfigurations().isEmpty()
                ? config : config);
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}