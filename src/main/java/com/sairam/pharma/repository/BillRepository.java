package com.sairam.pharma.repository;

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

    List<Bill> findByAgencyIdOrderByBillDateDesc(Long agencyId);

    List<Bill> findByAgencyIdAndBillDateBetweenOrderByBillDateDesc(
            Long agencyId, LocalDate startDate, LocalDate endDate
    );

    List<Bill> findByAgencyIdAndStatusOrderByBillDateDesc(Long agencyId, BillStatus status);

    @Query("""
        SELECT
            COALESCE(SUM(b.totalAmount), 0),
            COALESCE(SUM(b.paidAmount), 0),
            COALESCE(SUM(b.dueAmount), 0)
        FROM Bill b
        WHERE b.agency.id = :agencyId
        AND b.billDate BETWEEN :startDate AND :endDate
        """)
    Object[] getBillSummaryTotals(
            @Param("agencyId") Long agencyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT bi.bill.id, COUNT(bi)
        FROM BillItem bi
        WHERE bi.bill.id IN :billIds
        GROUP BY bi.bill.id
        """)
    List<Object[]> findItemCountsByBillIds(@Param("billIds") List<Long> billIds);

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

    @Query("SELECT COALESCE(SUM(b.dueAmount), 0) FROM Bill b WHERE b.dueAmount > 0")
    BigDecimal sumAllOutstandingDue();

    long countByDueAmountGreaterThan(BigDecimal amount);
}