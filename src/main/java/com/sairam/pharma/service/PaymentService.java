package com.sairam.pharma.service;

// ================================================================
// PaymentService.java  —  SERVICE LAYER
//
// THE CORE LOGIC OF "PAY A BILL":
//
//   1. User enters an amount (partial or full) + optional proof image
//   2. We create a Payment record
//   3. We add this amount to Bill.paidAmount
//   4. We recalculate Bill.dueAmount = totalAmount - paidAmount
//   5. We recalculate Bill.status (UNPAID / PARTIALLY_PAID / PAID)
//
// VALIDATION RULE:
//   You CANNOT pay more than the current due amount.
//   e.g. if due = ₹500, you can't record a ₹600 payment.
//   (This prevents negative due amounts / data corruption)
// ================================================================

import com.sairam.pharma.dto.PaymentDto;
import com.sairam.pharma.entity.Bill;
import com.sairam.pharma.entity.Payment;
import com.sairam.pharma.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillService billService;
    private final FileStorageService fileStorageService;

    @Transactional
    public PaymentDto.Response payBill(Long billId, PaymentDto.Request request) {

        Bill bill = billService.findBillOrThrow(billId);

        BigDecimal amountPaid;
        BigDecimal discountAmount;
        BigDecimal totalCleared;

        if (Boolean.TRUE.equals(request.getMarkAsFullyPaid())) {
            discountAmount = calculateDiscountAmount(
                    request.getDiscountType(), request.getDiscountValue(), bill.getDueAmount());
            amountPaid   = bill.getDueAmount().subtract(discountAmount).max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            totalCleared = bill.getDueAmount();

        } else {
            amountPaid = (request.getAmountPaid() != null
                    ? request.getAmountPaid() : BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);

            discountAmount = calculateDiscountAmount(
                    request.getDiscountType(), request.getDiscountValue(), bill.getDueAmount());

            totalCleared = amountPaid.add(discountAmount).setScale(2, RoundingMode.HALF_UP);

            if (amountPaid.compareTo(BigDecimal.ZERO) < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount paid cannot be negative");
            if (totalCleared.compareTo(BigDecimal.ZERO) <= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Total cleared (payment + discount) must be greater than zero");
            if (totalCleared.compareTo(bill.getDueAmount()) > 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Total cleared ₹" + totalCleared + " cannot exceed due amount ₹" + bill.getDueAmount());
        }

        LocalDate paymentDate = request.getPaymentDate() != null
                ? request.getPaymentDate() : LocalDate.now();

        Payment payment = Payment.builder()
                .amountPaid(amountPaid)
                .paymentDate(paymentDate)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .discountAmount(discountAmount)
                .totalCleared(totalCleared)
                .proofImageUrl(request.getProofImageUrl())
                .notes(request.getNotes())
                .build();

        bill.addPayment(payment);

        BigDecimal newPaidAmount = bill.getPaidAmount().add(totalCleared);
        BigDecimal newDueAmount  = bill.getDueAmount().subtract(totalCleared).max(BigDecimal.ZERO);
        bill.setPaidAmount(newPaidAmount);
        bill.setDueAmount(newDueAmount);
        bill.setStatus(BillService.calculateStatus(bill.getTotalAmount(), newPaidAmount, newDueAmount));

        try {
            billService.saveBill(bill);

            // ---- CONFIRM PROOF IMAGE ----
            // Payment saved → make proof image permanent in Cloudinary
            if (payment.getProofImageUrl() != null) {
                fileStorageService.confirmFile(payment.getProofImageUrl());
            }

        } catch (Exception e) {
            // ---- DELETE TEMP PROOF IMAGE ON FAILURE ----
            if (request.getProofImageUrl() != null) {
                try {
                    String publicId = fileStorageService.extractPublicId(request.getProofImageUrl());
                    if (publicId != null) fileStorageService.deleteFile(publicId);
                } catch (Exception ex) {
                    log.warn("Could not delete temp proof after payment failure: {}", ex.getMessage());
                }
            }
            throw e;
        }

        return PaymentDto.Response.builder()
                .id(payment.getId())
                .amountPaid(amountPaid)
                .paymentDate(paymentDate)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .discountAmount(discountAmount)
                .totalCleared(totalCleared)
                .proofImageUrl(payment.getProofImageUrl())
                .notes(payment.getNotes())
                .paidAt(payment.getPaidAt())
                .newDueAmount(newDueAmount)
                .billStatus(bill.getStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.Response> getPaymentsForBill(Long billId) {

        billService.findBillOrThrow(billId);

        return paymentRepository.findByBillIdOrderByPaidAtDesc(billId)
                .stream()
                .map(p -> PaymentDto.Response.builder()
                        .id(p.getId())
                        .amountPaid(p.getAmountPaid())
                        .paymentDate(p.getPaymentDate())
                        .discountType(p.getDiscountType())
                        .discountValue(p.getDiscountValue())
                        .discountAmount(p.getDiscountAmount())
                        .totalCleared(p.getTotalCleared())
                        .proofImageUrl(p.getProofImageUrl())
                        .notes(p.getNotes())
                        .paidAt(p.getPaidAt())
                        .build())
                .collect(Collectors.toList());
    }

    private BigDecimal calculateDiscountAmount(
            String discountType, BigDecimal discountValue, BigDecimal dueAmount) {

        if (discountType == null || discountValue == null
                || discountValue.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        return switch (discountType.toUpperCase()) {
            case "FIXED" ->
                    discountValue.min(dueAmount).setScale(2, RoundingMode.HALF_UP);
            case "PERCENTAGE" -> {
                BigDecimal pct = discountValue.min(new BigDecimal("100"));
                yield dueAmount.multiply(pct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
            default -> BigDecimal.ZERO;
        };
    }
}