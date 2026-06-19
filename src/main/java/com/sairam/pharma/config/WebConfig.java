package com.sairam.pharma.config;

// ================================================================
// WebConfig.java  —  STATIC RESOURCE CONFIGURATION
//
// WHAT IS THIS?
// Makes files in the "uploads" folder accessible via URL.
//
// WITHOUT THIS:
//   You save a file to uploads/bills/abc123.jpg
//   But http://localhost:8080/uploads/bills/abc123.jpg → 404 Not Found
//
// WITH THIS:
//   http://localhost:8080/uploads/bills/abc123.jpg → shows the image
//
// addResourceHandler  = "which URLs should serve files"
// addResourceLocations = "where on disk to look for those files"
//   "file:uploads/" means the "uploads" folder in your project root
// ================================================================

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
}