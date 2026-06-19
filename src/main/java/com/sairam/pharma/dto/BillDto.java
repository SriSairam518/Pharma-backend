package com.sairam.pharma.dto;

// ================================================================
// BillDto.java  —  DTO for a bill
//
// THREE response shapes for different screens:
//
//   Request        → creating/editing a bill (with item list)
//   Response        → full bill detail (with all items) — bill detail page
//   SummaryResponse → lightweight version for bill LISTS — no items array
//                     (faster — don't send 20 medicine rows for every
//                      bill in a list of 50 bills)
// ================================================================

// ================================================================
// BillDto.java  —  DTO for a bill
//
// IMPORTANT CHANGE: totalAmount now comes DIRECTLY from the scanned
// netAmount on the bill (the grand total printed on the invoice).
// We no longer calculate it by summing item amounts — too many
// real bills have rounding differences, extra charges, etc. that
// make item-sum != printed total. We trust what's printed.
// ================================================================

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

    // ============================================================
    // REQUEST  —  create or update a bill
    // ============================================================
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
        // Cannot be in the future — you can't receive a bill dated tomorrow
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate billDate;

        // Optional — set after uploading the bill image separately
        private String billImageUrl;

        // ---- SCANNED SUMMARY FIELDS (all optional — leave blank if not on bill) ----
        private BigDecimal subTotal;     // gross amount before discount/GST
        private BigDecimal billDiscount; // total bill-level discount
        private BigDecimal billGst;      // total GST amount

        // ---- NET AMOUNT — this becomes the bill's totalAmount ----
        // REQUIRED — this is what due/payment tracking is based on
        @NotNull(message = "Net amount (grand total) is required")
        private BigDecimal netAmount;

        // The editable medicine table — must have at least 1 row
        @NotEmpty(message = "A bill must have at least one medicine item")
        @Valid   // IMPORTANT: validates each item inside the list too
        private List<BillItemDto.Request> items;
    }

    // ============================================================
    // RESPONSE  —  full bill detail (used on the Bill Detail page)
    // ============================================================
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

    // ============================================================
    // SUMMARY RESPONSE  —  lightweight, used in bill LISTS
    // ============================================================
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

        // How many medicine line items this bill has (for display only)
        private Integer itemCount;
    }

    // ============================================================
    // AGENCY BILLS SUMMARY  —  used at the top of "Bills per agency" page
    // Shows total outstanding due across ALL bills in the selected range
    // ============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgencyBillsSummary {
        private Long agencyId;
        private String agencyName;
        private BigDecimal totalBilledAmount;  // sum of totalAmount
        private BigDecimal totalPaidAmount;    // sum of paidAmount
        private BigDecimal totalDueAmount;     // sum of dueAmount
        private Integer billCount;
        private List<SummaryResponse> bills;
    }
}