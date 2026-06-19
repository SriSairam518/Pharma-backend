package com.sairam.pharma.dto;

// ================================================================
// PaymentDto.java  —  DTO for recording a payment
//
// IMPORTANT: amountPaid must NOT have @NotNull.
// When the user clicks "Mark bill as fully paid", the frontend
// sends amountPaid as null (the backend calculates it from the
// due amount minus any discount). If @NotNull is present here,
// Spring rejects the request with "Validation failed" before it
// ever reaches PaymentService — which is exactly the bug you hit.
// ================================================================

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

        // NO @NotNull here on purpose — see note above.
        // Only validates the value IS NOT NEGATIVE when one is provided.
        // null is allowed through (markAsFullyPaid path relies on this).
        @DecimalMin(value = "0.00", message = "Amount paid cannot be negative")
        private BigDecimal amountPaid;

        // The date the user selects (when they actually paid)
        @NotNull(message = "Payment date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate paymentDate;

        // "FIXED" or "PERCENTAGE" — null means no discount
        private String discountType;

        @DecimalMin(value = "0.0", message = "Discount cannot be negative")
        private BigDecimal discountValue;

        private String proofImageUrl;

        @Size(max = 255, message = "Notes must not exceed 255 characters")
        private String notes;

        // ---- ONE-CLICK "MARK AS FULLY PAID" ----
        // When true: backend clears the ENTIRE due amount.
        // If discountType/discountValue are also provided, that discount
        // is applied first, and the remaining due becomes the amountPaid
        // automatically — user doesn't need to type anything else.
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