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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration  // tells Spring: "this class contains configuration beans"
public class CorsConfig {

    @Bean  // Spring will create and manage this object
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Which frontend URLs are allowed to call this backend
        // In production, change this to your actual deployed frontend URL
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",   // Vite dev server (React)
                "http://localhost:3000"    // Create React App (just in case)
        ));

        // Which HTTP methods are allowed
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Which request headers are allowed
        // "Authorization" is needed later when we add JWT login
        config.setAllowedHeaders(List.of("*"));

        // Allow the browser to send cookies / auth headers
        config.setAllowCredentials(true);

        // Apply this CORS config to ALL routes (/api/*)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}