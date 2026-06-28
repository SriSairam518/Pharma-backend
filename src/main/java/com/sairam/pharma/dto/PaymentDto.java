package com.sairam.pharma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @DecimalMin(value = "0.00", message = "Amount paid cannot be negative")
        private BigDecimal amountPaid;

        @NotNull(message = "Payment date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate paymentDate;

        private String discountType;

        @DecimalMin(value = "0.0", message = "Discount cannot be negative")
        private BigDecimal discountValue;

        private String proofImageUrl;

        @Size(max = 255, message = "Notes must not exceed 255 characters")
        private String notes;

        @Builder.Default
        private Boolean markAsFullyPaid = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private BigDecimal amountPaid;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate paymentDate;

        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal discountAmount;
        private BigDecimal totalCleared;

        private String proofImageUrl;
        private String notes;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime paidAt;

        private BigDecimal newDueAmount;
        private String     billStatus;
    }
}