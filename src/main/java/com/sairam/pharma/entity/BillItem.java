package com.sairam.pharma.entity;

// ================================================================
// BillItem.java  —  ENTITY
//
// WHAT IS THIS?
// One row of medicine on a bill.
// e.g. "Paracetamol 500mg, batch PCT2024A, expires Dec 2026,
//       qty 100, price ₹2.50 each, total ₹250"
//
// This is the table that gets generated after OCR scans the bill.
// Later (Phase 5), these items will ALSO update the Inventory table
// — but that's a separate concern we'll connect later.
// ================================================================

// ================================================================
// BillItem.java  —  ENTITY  (UPDATED — real pharma invoice columns)
//
// Matches exactly what's printed on a real Indian pharma invoice:
// HSN | Product Name | Pack | Batch No | Expiry | Qty | MRP | Rate | Disc | GST | Amount
//
// IMPORTANT: amount, MRP, rate, discount, GST are all SCANNED values
// from the bill — we do NOT calculate them. Whatever OCR reads is
// stored exactly as-is (as text where the format varies, e.g. GST
// could be "12%" or "₹15.50" depending on how the bill prints it).
// ================================================================

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bill_items")
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- RELATIONSHIP: many items belong to one bill ----
    // @JsonIgnore not needed here because we won't serialize this
    // field directly — we map BillItem → BillItemDto manually
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    // ---- HSN code (Harmonized System Nomenclature — tax classification) ----
    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    // ---- MEDICINE DETAILS ----

    // ---- Product / medicine name ----
    @Column(name = "medicine_name", nullable = false, length = 150)
    private String medicineName;

    // ---- Pack size (e.g. "10*10", "1*100ML", "10S") ----
    @Column(length = 30)
    private String pack;

    // Batch number printed on the medicine strip — important for
    // tracking recalls and expiry per batch
    // ---- Batch number ----
    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    // ---- Expiry date ----
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // ---- Quantity ----
    // NOTE: if the bill shows "10 + 5" (paid + free), OCR combines
    // them into a single total quantity (e.g. 15) before saving.
    // Decimal allowed because some bills show fractional qty (e.g. 4.5 + 0.5 = 5)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    // ---- MRP (Maximum Retail Price) — scanned, not calculated ----
    @Column(precision = 12, scale = 2)
    private BigDecimal mrp;

    // ---- Rate (the actual purchase rate per unit) — scanned ----
    @Column(precision = 12, scale = 2)
    private BigDecimal rate;

    // ---- Discount — stored as scanned text (could be "5%" or "₹10") ----
    @Column(length = 20)
    private String discount;

    // ---- GST — stored as scanned text (could be "12%" or "₹15.50") ----
    @Column(name = "gst", length = 20)
    private String gst;

    // ---- Amount — the final line amount as PRINTED on the bill ----
    // This is SCANNED directly, never calculated from qty × rate
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;
}
