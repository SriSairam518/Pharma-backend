package com.sairam.pharma.repository;

// ================================================================
// PaymentRepository.java  —  REPOSITORY
// ================================================================

import com.sairam.pharma.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Get all payments made against one bill, most recent first
    // SELECT * FROM payments WHERE bill_id = ? ORDER BY paid_at DESC
    List<Payment> findByBillIdOrderByPaidAtDesc(Long billId);
}