package com.sairam.pharma.controller;

// ================================================================
// UploadController.java  —  CONTROLLER
//
// ENDPOINTS:
//   POST /api/uploads/bill     → upload a bill image, get back a URL
//   POST /api/uploads/payment  → upload a payment proof image
//
// FRONTEND FLOW:
//   1. User selects a bill image file
//   2. Frontend uploads it HERE first → gets back a URL
//      e.g. "/uploads/bills/abc-123.jpg"
//   3. Frontend then sends that URL as part of the
//      "create bill" request (billImageUrl field)
//
// This is a 2-step process: upload file → get URL → use URL in data.
// This is the standard pattern for handling file uploads with JSON APIs.
// ================================================================

import com.sairam.pharma.dto.ApiResponse;
import com.sairam.pharma.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    // ----------------------------------------------------------------
    // POST /api/uploads/bill
    // Form-data key: "file"
    // ----------------------------------------------------------------
    @PostMapping("/api/uploads/bill")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBillImage(
            @RequestParam("file") MultipartFile file
    ) {
        validateImage(file);
        String url = fileStorageService.storeFile(file, "bills");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", Map.of("url", url)));
    }

    // ----------------------------------------------------------------
    // POST /api/uploads/payment
    // Form-data key: "file"
    // ----------------------------------------------------------------
    @PostMapping("/api/uploads/payment")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPaymentProof(
            @RequestParam("file") MultipartFile file
    ) {
        validateImage(file);
        String url = fileStorageService.storeFile(file, "payments");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", Map.of("url", url)));
    }

    // ---- Basic validation ----
    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        boolean isImage = contentType != null && contentType.startsWith("image/");
        boolean isPdf   = contentType != null && contentType.equals("application/pdf");

        if (!isImage && !isPdf) {
            throw new IllegalArgumentException("Only image or PDF files are allowed");
        }

        // 10MB limit (matches application.properties multipart config)
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must not exceed 10MB");
        }
    }
}