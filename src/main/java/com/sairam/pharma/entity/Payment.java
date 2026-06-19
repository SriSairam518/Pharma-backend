package com.sairam.pharma.entity;

// ================================================================
// Payment.java  —  ENTITY
//
// WHAT IS THIS?
// One payment record. A single bill can have MULTIPLE payments
// over time (you might pay ₹5,000 today and ₹10,000 next week).
//
// Every time a Payment is saved:
//   1. Bill.paidAmount += payment.amountPaid
//   2. Bill.dueAmount  = Bill.totalAmount - Bill.paidAmount
//   3. Bill.status updates based on the new dueAmount
//
// This logic lives in PaymentService (Layer 2), NOT here.
// Entities should stay "dumb" — just data, no business logic.
// ================================================================

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    // How much was actually paid in cash/UPI (AFTER discount)
    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    // ---- DISCOUNT FIELDS ----

    // "FIXED" or "PERCENTAGE" — null means no discount
    @Column(name = "discount_type", length = 15)
    private String discountType;

    // The value entered: e.g. 500 (fixed) or 5 (percentage)
    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    // The actual rupee amount of discount (calculated and stored)
    // e.g. if 5% on ₹10,000 → discountAmount = 500.00
    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // amountPaid + discountAmount = total cleared from due
    // e.g. paid ₹9,500 + discount ₹500 = ₹10,000 cleared from due
    @Column(name = "total_cleared", precision = 12, scale = 2)
    private BigDecimal totalCleared;

    @Column(name = "payment_date", precision = 12, scale = 2)
    private LocalDate paymentDate;

    @Column(name = "proof_image_url", length = 255)
    private String proofImageUrl;

    @Column(length = 255)
    private String notes;

    @CreatedDate
    @Column(name = "paid_at", nullable = false, updatable = false)
    private LocalDateTime paidAt;
}
