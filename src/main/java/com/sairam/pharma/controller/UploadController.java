package com.sairam.pharma.controller;

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

        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must not exceed 10MB");
        }
    }
}