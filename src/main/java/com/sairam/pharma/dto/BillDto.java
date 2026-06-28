package com.sairam.pharma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sairam.pharma.entity.BillStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class BillDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotNull(message = "Agency is required")
        private Long agencyId;

        @NotBlank(message = "Bill number is required")
        private String billNumber;

        @NotNull(message = "Bill date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate billDate;

        private String billImageUrl;

        private BigDecimal subTotal;
        private BigDecimal billDiscount;
        private BigDecimal billGst;

        @NotNull(message = "Net amount (grand total) is required")
        private BigDecimal netAmount;

        @NotEmpty(message = "A bill must have at least one medicine item")
        @Valid
        private List<BillItemDto.Request> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long agencyId;
        private String agencyName;
        private String billNumber;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate billDate;

        private BigDecimal subTotal;
        private BigDecimal billDiscount;
        private BigDecimal billGst;
        private BigDecimal netAmount;

        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal dueAmount;
        private BillStatus status;
        private String billImageUrl;

        private List<BillItemDto.Response> items;
        private List<PaymentDto.Response> payments;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse {
        private Long id;
        private String billNumber;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate billDate;

        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal dueAmount;
        private BillStatus status;

        private Integer itemCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgencyBillsSummary {
        private Long agencyId;
        private String agencyName;
        private BigDecimal totalBilledAmount;
        private BigDecimal totalPaidAmount;
        private BigDecimal totalDueAmount;
        private Integer billCount;
        private List<SummaryResponse> bills;
    }
}