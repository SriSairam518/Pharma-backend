package com.sairam.pharma.service;

// ================================================================
// BillService.java  —  SERVICE LAYER
//
// RESPONSIBILITIES:
//   1. Create a bill with its medicine items (from the OCR-edited table)
//   2. Auto-calculate totalAmount = sum of (quantity × unitPrice)
//   3. Fetch bills for an agency, filtered by date range
//   4. Build the "agency bills summary" (total due card)
//   5. Update / delete bills
//
// IMPORTANT CONCEPT — "last X days" date range:
//   "Last 7 days" means: from (today - 7 days) to today
//   We calculate this in the SERVICE, not the controller,
//   because date math is business logic.
// ================================================================

// ================================================================
// BillService.java  —  SERVICE LAYER
//
// KEY CHANGE: We NO LONGER calculate totalAmount by summing items.
// Real pharma bills often have rounding adjustments, extra charges,
// or item amounts that don't perfectly sum to the printed total.
// We TRUST the scanned netAmount as the source of truth for
// totalAmount/dueAmount/payment tracking.
//
// Item-level fields (amount, MRP, rate, discount, GST) are stored
// exactly as scanned — purely for reference/display, never used
// in any calculation.
// ================================================================

import com.sairam.pharma.dto.BillDto;
import com.sairam.pharma.dto.BillItemDto;
import com.sairam.pharma.dto.PaymentDto;
import com.sairam.pharma.entity.Agency;
import com.sairam.pharma.entity.Bill;
import com.sairam.pharma.entity.BillItem;
import com.sairam.pharma.entity.BillStatus;
import com.sairam.pharma.exception.ResourceNotFoundException;
import com.sairam.pharma.repository.AgencyRepository;
import com.sairam.pharma.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final AgencyRepository agencyRepository;

    // ---- CREATE BILL ----
    @Transactional
    public BillDto.Response createBill(BillDto.Request request) {

        Agency agency = agencyRepository.findById(request.getAgencyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agency not found with id: " + request.getAgencyId()
                ));

        // netAmount (scanned grand total) becomes totalAmount.
        // Nothing is calculated — we trust what's printed on the bill.
        BigDecimal netAmount = request.getNetAmount().setScale(2, RoundingMode.HALF_UP);

        Bill bill = Bill.builder()
                .agency(agency)
                .billNumber(request.getBillNumber().trim())
                .billDate(request.getBillDate())
                .billImageUrl(request.getBillImageUrl())
                .subTotal(request.getSubTotal())
                .billDiscount(request.getBillDiscount())
                .billGst(request.getBillGst())
                .netAmount(netAmount)
                .totalAmount(netAmount)        // ← scanned value, not calculated
                .paidAmount(BigDecimal.ZERO)
                .dueAmount(netAmount)           // nothing paid yet → due = full net amount
                .status(BillStatus.UNPAID)
                .build();

        // Convert each item DTO → BillItem entity — ALL fields scanned, nothing computed
        for (BillItemDto.Request itemReq : request.getItems()) {
            BillItem item = BillItem.builder()
                    .hsnCode(itemReq.getHsnCode())
                    .medicineName(itemReq.getMedicineName().trim())
                    .pack(itemReq.getPack())
                    .batchNumber(itemReq.getBatchNumber())
                    .expiryDate(itemReq.getExpiryDate())
                    .quantity(BigDecimal.valueOf(itemReq.getQuantity()))
                    .mrp(itemReq.getMrp())
                    .rate(itemReq.getRate())
                    .discount(itemReq.getDiscount())
                    .gst(itemReq.getGst())
                    .amount(itemReq.getAmount())   // scanned line amount, not qty×rate
                    .build();

            bill.addItem(item);
        }

        Bill saved = billRepository.save(bill);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BillDto.Response getBillById(Long id) {
        return toResponse(findBillOrThrow(id));
    }

    @Transactional(readOnly = true)
    public BillDto.AgencyBillsSummary getAgencyBills(
            Long agencyId, Integer days, LocalDate customFrom, LocalDate customTo) {

        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agency not found with id: " + agencyId
                ));

        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;

        if (customFrom != null && customTo != null) {
            startDate = customFrom;
            endDate = customTo;
        } else if (days != null) {
            startDate = today.minusDays(days);
        } else {
            startDate = LocalDate.of(2000, 1, 1);
        }

        List<Bill> bills = billRepository
                .findByAgencyIdAndBillDateBetweenOrderByBillDateDesc(agencyId, startDate, endDate);

        BigDecimal totalBilled = billRepository.sumTotalAmountByAgencyAndDateRange(agencyId, startDate, endDate);
        BigDecimal totalPaid   = billRepository.sumPaidAmountByAgencyAndDateRange(agencyId, startDate, endDate);
        BigDecimal totalDue    = billRepository.sumDueAmountByAgencyAndDateRange(agencyId, startDate, endDate);

        List<BillDto.SummaryResponse> billSummaries = bills.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());

        return BillDto.AgencyBillsSummary.builder()
                .agencyId(agency.getId())
                .agencyName(agency.getName())
                .totalBilledAmount(totalBilled)
                .totalPaidAmount(totalPaid)
                .totalDueAmount(totalDue)
                .billCount(bills.size())
                .bills(billSummaries)
                .build();
    }

    // ---- UPDATE BILL ----
    @Transactional
    public BillDto.Response updateBill(Long id, BillDto.Request request) {
        Bill bill = findBillOrThrow(id);

        bill.setBillNumber(request.getBillNumber().trim());
        bill.setBillDate(request.getBillDate());
        if (request.getBillImageUrl() != null) {
            bill.setBillImageUrl(request.getBillImageUrl());
        }

        bill.setSubTotal(request.getSubTotal());
        bill.setBillDiscount(request.getBillDiscount());
        bill.setBillGst(request.getBillGst());

        BigDecimal newNetAmount = request.getNetAmount().setScale(2, RoundingMode.HALF_UP);
        bill.setNetAmount(newNetAmount);
        bill.setTotalAmount(newNetAmount);

        // Recalculate due: new total - whatever was already paid
        BigDecimal newDue = newNetAmount.subtract(bill.getPaidAmount());
        bill.setDueAmount(newDue.max(BigDecimal.ZERO));
        bill.setStatus(calculateStatus(newNetAmount, bill.getPaidAmount(), bill.getDueAmount()));

        // Replace all items
        bill.getItems().clear();
        for (BillItemDto.Request itemReq : request.getItems()) {
            BillItem item = BillItem.builder()
                    .hsnCode(itemReq.getHsnCode())
                    .medicineName(itemReq.getMedicineName().trim())
                    .pack(itemReq.getPack())
                    .batchNumber(itemReq.getBatchNumber())
                    .expiryDate(itemReq.getExpiryDate())
                    .quantity(BigDecimal.valueOf(itemReq.getQuantity()))
                    .mrp(itemReq.getMrp())
                    .rate(itemReq.getRate())
                    .discount(itemReq.getDiscount())
                    .gst(itemReq.getGst())
                    .amount(itemReq.getAmount())
                    .build();
            bill.addItem(item);
        }

        Bill updated = billRepository.save(bill);
        return toResponse(updated);
    }

    @Transactional
    public void deleteBill(Long id) {
        billRepository.delete(findBillOrThrow(id));
    }

    // ================================================================
    // STATUS CALCULATION
    // ================================================================
    public static BillStatus calculateStatus(BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal dueAmount) {
        if (dueAmount.compareTo(BigDecimal.ZERO) <= 0) return BillStatus.PAID;
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) return BillStatus.UNPAID;
        return BillStatus.PARTIALLY_PAID;
    }

    // ---- SAVE BILL (used by PaymentService) ----
    @Transactional
    public Bill saveBill(Bill bill) {
        return billRepository.save(bill);
    }

    // Package-private — used by PaymentService
    Bill findBillOrThrow(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }

    private BillDto.Response toResponse(Bill bill) {
        List<BillItemDto.Response> itemDtos = bill.getItems().stream()
                .map(item -> BillItemDto.Response.builder()
                        .id(item.getId())
                        .hsnCode(item.getHsnCode())
                        .medicineName(item.getMedicineName())
                        .pack(item.getPack())
                        .batchNumber(item.getBatchNumber())
                        .expiryDate(item.getExpiryDate())
                        .quantity(item.getQuantity())
                        .mrp(item.getMrp())
                        .rate(item.getRate())
                        .discount(item.getDiscount())
                        .gst(item.getGst())
                        .amount(item.getAmount())
                        .build())
                .collect(Collectors.toList());

        List<PaymentDto.Response> paymentDtos = bill.getPayments().stream()
                .sorted(Comparator.comparing(com.sairam.pharma.entity.Payment::getPaidAt).reversed())
                .map(payment -> PaymentDto.Response.builder()
                        .id(payment.getId())
                        .amountPaid(payment.getAmountPaid())
                        .paymentDate(payment.getPaymentDate())
                        .discountType(payment.getDiscountType())
                        .discountValue(payment.getDiscountValue())
                        .discountAmount(payment.getDiscountAmount())
                        .totalCleared(payment.getTotalCleared())
                        .proofImageUrl(payment.getProofImageUrl())
                        .notes(payment.getNotes())
                        .paidAt(payment.getPaidAt())
                        .build())
                .collect(Collectors.toList());

        return BillDto.Response.builder()
                .id(bill.getId())
                .agencyId(bill.getAgency().getId())
                .agencyName(bill.getAgency().getName())
                .billNumber(bill.getBillNumber())
                .billDate(bill.getBillDate())
                .subTotal(bill.getSubTotal())
                .billDiscount(bill.getBillDiscount())
                .billGst(bill.getBillGst())
                .netAmount(bill.getNetAmount())
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .dueAmount(bill.getDueAmount())
                .status(bill.getStatus())
                .billImageUrl(bill.getBillImageUrl())
                .items(itemDtos)
                .payments(paymentDtos)
                .createdAt(bill.getCreatedAt())
                .build();
    }

    private BillDto.SummaryResponse toSummaryResponse(Bill bill) {
        return BillDto.SummaryResponse.builder()
                .id(bill.getId())
                .billNumber(bill.getBillNumber())
                .billDate(bill.getBillDate())
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .dueAmount(bill.getDueAmount())
                .status(bill.getStatus())
                .itemCount(bill.getItems().size())
                .build();
    }
}