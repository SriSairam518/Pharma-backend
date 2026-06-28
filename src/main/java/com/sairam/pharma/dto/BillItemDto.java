package com.sairam.pharma.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillItemDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Medicine name is required")
        @Size(max = 150, message = "Medicine name must not exceed 150 characters")
        private String medicineName;

        private String hsnCode;
        private String pack;
        private String batchNumber;
        private LocalDate expiryDate;
        private Integer quantity;

        private BigDecimal mrp;
        private BigDecimal rate;
        private String discount;
        private String gst;
        private BigDecimal amount;
    }

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