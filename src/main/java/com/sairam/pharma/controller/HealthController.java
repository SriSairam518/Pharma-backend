package com.sairam.pharma.controller;

// ================================================================
// HealthController.java
//
// WHY THIS EXISTS:
// Render (and most cloud platforms) periodically "ping" a URL on
// your app to check if it's alive. If it doesn't get a 200 OK
// response, Render thinks your app crashed and may restart it
// or mark the deployment as failed.
//
// This endpoint requires NO database, NO Cloudinary, NO external
// calls — just confirms the Spring Boot process itself is running
// and accepting HTTP requests. Keeping it dependency-free means
// it stays healthy even if, say, the database has a brief hiccup.
// ================================================================

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    // Render's free tier pings this URL periodically.
    // Also useful for you to quickly check "is my backend up?"
    // by just visiting https://your-app.onrender.com/health in a browser.
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}