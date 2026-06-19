package com.sairam.pharma.entity;

// ================================================================
// Bill.java  —  ENTITY
//
// WHAT IS THIS?
// Represents ONE bill/invoice received from an agency.
// e.g. "Sun Pharma sent us bill #INV-2024-105 on 12 June for ₹15,000"
//
// RELATIONSHIPS EXPLAINED:
//
// 1. Bill → Agency  (MANY bills belong to ONE agency)
//    @ManyToOne — "many bills point to one agency"
//    ANALOGY: many students (bills) belong to one school (agency)
//
// 2. Bill → BillItem  (ONE bill has MANY medicine line items)
//    @OneToMany — "one bill has many items"
//    ANALOGY: one invoice (bill) has many line items (medicines)
//
// 3. Bill → Payment  (ONE bill can have MANY payments — partial payments)
//    @OneToMany — same idea
//
// CASCADE = ALL means: if you save/delete a Bill, automatically
// save/delete its BillItems and Payments too. You don't manage
// them separately.
//
// MONEY FIELDS use BigDecimal — NEVER use float/double for money!
// float/double have rounding errors (0.1 + 0.2 != 0.3 in binary).
// BigDecimal is exact — the correct choice for currency.
// ================================================================

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bills")
@EntityListeners(AuditingEntityListener.class)
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- RELATIONSHIP: many bills belong to one agency ----
    // @ManyToOne   = the "many" side of the relationship
    // @JoinColumn  = creates a column "agency_id" in the bills table
    //                that stores the foreign key (FK) to agencies.id
    // fetch = LAZY = don't load the full Agency object unless we ask for it
    //                (better performance — avoids loading unnecessary data)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    // ---- BILL DETAILS ----

    // The invoice number printed on the physical bill (e.g. "INV-2024-105")
    @Column(name = "bill_number", nullable = false, length = 50)
    private String billNumber;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    // ---- MONEY FIELDS (always BigDecimal, precision 12, scale 2) ----
    // precision=12 → max 12 digits total
    // scale=2      → 2 digits after the decimal point (paise)
    // e.g. 9999999999.99 is the max value
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // ---- SCANNED BILL SUMMARY FIELDS ----
    // These come DIRECTLY from OCR — never calculated from items.
    // The bill prints these as its own summary block (usually bottom-right).

    // Sub total / gross amount (before discount and GST)
    @Column(name = "sub_total", precision = 12, scale = 2)
    private BigDecimal subTotal;

    // Total discount applied across the whole bill (scanned, not item-level)
    @Column(name = "bill_discount", precision = 12, scale = 2)
    private BigDecimal billDiscount;

    // Total GST amount on the bill
    @Column(name = "bill_gst", precision = 12, scale = 2)
    private BigDecimal billGst;

    // Net amount / grand total — THIS becomes totalAmount above.
    // Kept as a separate field too so we always know exactly what
    // OCR scanned vs. what we're using for due/payment tracking.
    @Column(name = "net_amount", precision = 12, scale = 2)
    private  BigDecimal netAmount;

    // How much has been paid so far (sum of all Payment records)
    // Starts at 0.00 when bill is created
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    // totalAmount - paidAmount (recalculated every time a payment is made)
    @Column(name = "due_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal dueAmount;

    // ---- STATUS ----
    // @Enumerated(EnumType.STRING) stores "UNPAID" (text) not 0 (number)
    // in the database — much more readable when you look at the table directly
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BillStatus status = BillStatus.UNPAID;

    // ---- BILL IMAGE ----
    // Path to the uploaded bill image (e.g. "/uploads/bills/bill_123.jpg")
    // Nullable because a bill could theoretically be entered manually
    @Column(name = "bill_image_url", length = 255)
    private String billImageUrl;

    // ---- RELATIONSHIPS: one bill has many items and many payments ----

    // mappedBy = "bill" means: "look at the 'bill' field inside BillItem
    //            to find which bills these items belong to"
    // cascade = ALL means: saving/deleting a Bill also saves/deletes its items
    // orphanRemoval = true means: if an item is removed from this list,
    //                 delete it from the database too (not just unlink it)
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore // we expose items via DTO, not directly from the entity
    private List<BillItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();

    // ---- AUDIT FIELDS ----
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============================================================
    // HELPER METHOD — keeps the bidirectional relationship in sync
    //
    // WHY DO WE NEED THIS?
    // When you add a BillItem to this bill's `items` list, JPA also
    // needs the BillItem's `bill` field to point back to this Bill.
    // Without this, JPA won't know which bill the item belongs to
    // and won't save the foreign key correctly.
    // ============================================================

    public void addItem(BillItem item){
        items.add(item);
        item.setBill(this);  // set the "back-reference"
    }

    public void addPayment(Payment payment){
        payments.add(payment);
        payment.setBill(this);
    }
}
