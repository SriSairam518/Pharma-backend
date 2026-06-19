package com.sairam.pharma.dto;

// ================================================================
// BillItemDto.java  —  DTO for one medicine line item
//
// This is the shape of ONE ROW in the "editable medicine table"
// that appears after OCR scans a bill.
//
// FLOW:
//   1. User uploads bill image
//   2. OCR extracts rows → each becomes a BillItemDto.Request
//   3. Frontend shows editable table (user can fix OCR mistakes)
//   4. User clicks "Save" → all rows sent as List<BillItemDto.Request>
// ================================================================

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillItemDto {

    // ============================================================
    // REQUEST  —  one row sent from frontend to backend
    // ============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        // Only the medicine name is required — everything else can
        // be blank if OCR couldn't read it or the bill doesn't show it
        @NotBlank(message = "Medicine name is required")
        @Size(max = 150, message = "Medicine name must not exceed 150 characters")
        private String medicineName;

        private String hsnCode;
        private String pack;
        private String batchNumber;
        // Optional — some bills don't print expiry per item
        private LocalDate expiryDate;

        // Quantity — combined value if bill shows "paid + free"
        // e.g. bill shows "10 + 5" → frontend sends 15
        private Integer quantity;

        private BigDecimal mrp;
        private BigDecimal rate;
        private String discount;
        private String gst;
        private BigDecimal amount;
    }

    // ============================================================
    // RESPONSE  —  one row sent back to frontend
    // ============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String hsnCode;
        private String medicineName;
        private String pack;
        private String batchNumber;
        private LocalDate expiryDate;
        private BigDecimal quantity;
        private BigDecimal mrp;
        private BigDecimal rate;
        private String discount;
        private String gst;
        private BigDecimal amount;
    }
}