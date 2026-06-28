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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Column(name = "bill_number", nullable = false, length = 50)
    private String billNumber;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "sub_total", precision = 12, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "bill_discount", precision = 12, scale = 2)
    private BigDecimal billDiscount;

    @Column(name = "bill_gst", precision = 12, scale = 2)
    private BigDecimal billGst;

    @Column(name = "net_amount", precision = 12, scale = 2)
    private  BigDecimal netAmount;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal dueAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BillStatus status = BillStatus.UNPAID;

    @Column(name = "bill_image_url", length = 255)
    private String billImageUrl;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<BillItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addItem(BillItem item){
        items.add(item);
        item.setBill(this);
    }

    public void addPayment(Payment payment){
        payments.add(payment);
        payment.setBill(this);
    }
}
