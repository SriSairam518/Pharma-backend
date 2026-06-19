package com.sairam.pharma.repository;

// ================================================================
// BillRepository.java  —  REPOSITORY
//
// KEY QUERIES WE NEED:
//   1. Get all bills for an agency, optionally filtered by date range
//   2. Sum totals for the "due summary" at top of the bills page
//
// We use @Query (JPQL) for anything Spring can't auto-generate
// from the method name alone — like SUM() aggregations.
// ================================================================

import com.sairam.pharma.entity.Bill;
import com.sairam.pharma.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    // ---- Get all bills for one agency, newest first ----
    // Spring auto-generates:
    // SELECT * FROM bills WHERE agency_id = ? ORDER BY bill_date DESC
    List<Bill> findByAgencyIdOrderByBillDateDesc(Long agencyId);

    // ---- Get bills for one agency WITHIN a date range ----
    // Used for "last 7 / 30 / 60 days" and custom range filters
    // Spring auto-generates:
    // SELECT * FROM bills
    // WHERE agency_id = ? AND bill_date BETWEEN ? AND ?
    // ORDER BY bill_date DESC
    List<Bill> findByAgencyIdAndBillDateBetweenOrderByBillDateDesc(
            Long agencyId, LocalDate startDate, LocalDate endDate
    );

    // ---- Get bills by status for an agency (e.g. all unpaid bills) ----
    List<Bill> findByAgencyIdAndStatusOrderByBillDateDesc(Long agencyId, BillStatus status);

    // ---- SUM aggregations for the agency summary card ----
    // COALESCE(SUM(...), 0) → if there are no bills, return 0 instead of NULL
    // This avoids NullPointerException when no bills exist yet
    @Query("""
        SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b
        WHERE b.agency.id = :agencyId
        AND b.billDate BETWEEN :startDate AND :endDate
        """)
    BigDecimal sumTotalAmountByAgencyAndDateRange(
            @Param("agencyId") Long agencyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT COALESCE(SUM(b.paidAmount), 0) FROM Bill b
        WHERE b.agency.id = :agencyId
        AND b.billDate BETWEEN :startDate AND :endDate
        """)
    BigDecimal sumPaidAmountByAgencyAndDateRange(
            @Param("agencyId") Long agencyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT COALESCE(SUM(b.dueAmount), 0) FROM Bill b
        WHERE b.agency.id = :agencyId
        AND b.billDate BETWEEN :startDate AND :endDate
        """)
    BigDecimal sumDueAmountByAgencyAndDateRange(
            @Param("agencyId") Long agencyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ---- Total outstanding due across ALL agencies (for dashboard) ----
    @Query("SELECT COALESCE(SUM(b.dueAmount), 0) FROM Bill b WHERE b.dueAmount > 0")
    BigDecimal sumAllOutstandingDue();

    // ---- Count of bills with due > 0 (for dashboard stat) ----
    long countByDueAmountGreaterThan(BigDecimal amount);
}