package com.sairam.pharma.entity;

// ================================================================
// BillStatus.java  —  ENUM
//
// WHAT IS AN ENUM?
// A fixed list of named constants. Instead of writing "unpaid",
// "Unpaid", "UNPAID" in different places (typo-prone), we define
// these 3 values ONCE and reuse them everywhere.
//
// Spring Boot stores this as a VARCHAR in MySQL (we configure
// this with @Enumerated(EnumType.STRING) in the Bill entity).
// ================================================================

public enum BillStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID
}
