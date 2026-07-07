package com.sairam.pharma.service;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final AgencyRepository agencyRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public BillDto.Response createBill(BillDto.Request request) {

        Agency agency = agencyRepository.findById(request.getAgencyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agency not found with id: " + request.getAgencyId()));

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
                .totalAmount(netAmount)
                .paidAmount(BigDecimal.ZERO)
                .dueAmount(netAmount)
                .status(BillStatus.UNPAID)
                .build();

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

        try {
            Bill saved = billRepository.save(bill);
            if (saved.getBillImageUrl() != null) {
                fileStorageService.confirmFile(saved.getBillImageUrl());
            }
            return toResponse(saved);

        } catch (Exception e) {
            if (request.getBillImageUrl() != null) {
                try {
                    String publicId = fileStorageService.extractPublicId(request.getBillImageUrl());
                    if (publicId != null) fileStorageService.deleteFile(publicId);
                } catch (Exception ex) {
                    log.warn("Could not delete temp image after bill save failure: {}", ex.getMessage());
                }
            }
            throw e;
        }
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
                        "Agency not found with id: " + agencyId));

        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;

        if (customFrom != null && customTo != null) {
            startDate = customFrom;
            endDate   = customTo;
        } else if (days != null) {
            startDate = today.minusDays(days);
        } else {
            startDate = LocalDate.of(2000, 1, 1);
        }

        // Query 1 — bills list
        List<Bill> bills = billRepository
                .findByAgencyIdAndBillDateBetweenOrderByBillDateDesc(agencyId, startDate, endDate);

        // Query 2 — combined totals (3 SUMs in 1 query)
        // toBigDecimal() handles the case where Hibernate returns Integer/Long
        // instead of BigDecimal when the SUM result is 0 and there are no rows.
        // Without this, (BigDecimal) sums[0] throws ClassCastException → 500.
        Object[] sums        = billRepository.getBillSummaryTotals(agencyId, startDate, endDate);
        BigDecimal totalBilled = toBigDecimal(sums[0]);
        BigDecimal totalPaid   = toBigDecimal(sums[1]);
        BigDecimal totalDue    = toBigDecimal(sums[2]);

        // Query 3 — item counts for all bills at once (avoids N+1)
        Map<Long, Long> itemCounts = getItemCountsForBills(bills);

        List<BillDto.SummaryResponse> billSummaries = bills.stream()
                .map(bill -> toSummaryResponse(bill, itemCounts))
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

    // ================================================================
    // SAFE CAST helper
    //
    // WHY THIS EXISTS:
    // When a SUM() query has matching rows, Hibernate returns BigDecimal.
    // When there are NO matching rows, COALESCE(SUM(...), 0) returns
    // the literal 0 — which Hibernate maps as Integer, not BigDecimal.
    // Direct casting with (BigDecimal) sums[0] then throws ClassCastException.
    //
    // This method handles all numeric types Hibernate might return
    // and converts them safely to BigDecimal.
    // ================================================================
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        return BigDecimal.ZERO;
    }

    // Batch item count fetch — ONE query for all bills (avoids N+1)
    private Map<Long, Long> getItemCountsForBills(List<Bill> bills) {
        if (bills.isEmpty()) return Map.of();

        List<Long> billIds = bills.stream()
                .map(Bill::getId)
                .collect(Collectors.toList());

        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : billRepository.findItemCountsByBillIds(billIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }

        // Bills with zero items won't appear in GROUP BY result — default to 0
        for (Bill bill : bills) {
            counts.putIfAbsent(bill.getId(), 0L);
        }

        return counts;
    }

    @Transactional
    public BillDto.Response updateBill(Long id, BillDto.Request request) {
        Bill bill = findBillOrThrow(id);
        String oldImageUrl = bill.getBillImageUrl();

        bill.setBillNumber(request.getBillNumber().trim());
        bill.setBillDate(request.getBillDate());
        bill.setSubTotal(request.getSubTotal());
        bill.setBillDiscount(request.getBillDiscount());
        bill.setBillGst(request.getBillGst());

        BigDecimal newNetAmount = request.getNetAmount().setScale(2, RoundingMode.HALF_UP);
        bill.setNetAmount(newNetAmount);
        bill.setTotalAmount(newNetAmount);

        BigDecimal newDue = newNetAmount.subtract(bill.getPaidAmount());
        bill.setDueAmount(newDue.max(BigDecimal.ZERO));
        bill.setStatus(calculateStatus(newNetAmount, bill.getPaidAmount(), bill.getDueAmount()));

        if (request.getBillImageUrl() != null
                && !request.getBillImageUrl().equals(oldImageUrl)) {
            bill.setBillImageUrl(request.getBillImageUrl());
        }

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

        if (request.getBillImageUrl() != null
                && !request.getBillImageUrl().equals(oldImageUrl)) {
            fileStorageService.confirmFile(request.getBillImageUrl());
        }

        return toResponse(updated);
    }

    @Transactional
    public void deleteBill(Long id) {
        Bill bill = findBillOrThrow(id);

        List<String> imageUrlsToDelete = new java.util.ArrayList<>();
        if (bill.getBillImageUrl() != null) {
            imageUrlsToDelete.add(bill.getBillImageUrl());
        }
        bill.getPayments().forEach(payment -> {
            if (payment.getProofImageUrl() != null) {
                imageUrlsToDelete.add(payment.getProofImageUrl());
            }
        });

        billRepository.delete(bill);

        for (String imageUrl : imageUrlsToDelete) {
            try {
                String publicId = fileStorageService.extractPublicId(imageUrl);
                if (publicId != null) fileStorageService.deleteFile(publicId);
            } catch (Exception e) {
                log.warn("Could not delete Cloudinary image after bill deletion: {}",
                        e.getMessage());
            }
        }
    }

    public static BillStatus calculateStatus(
            BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal dueAmount) {
        if (dueAmount.compareTo(BigDecimal.ZERO) <= 0)  return BillStatus.PAID;
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) return BillStatus.UNPAID;
        return BillStatus.PARTIALLY_PAID;
    }

    @Transactional
    public Bill saveBill(Bill bill) {
        return billRepository.save(bill);
    }

    Bill findBillOrThrow(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bill not found with id: " + id));
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
                .sorted(Comparator.comparing(
                        com.sairam.pharma.entity.Payment::getPaidAt).reversed())
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

    private BillDto.SummaryResponse toSummaryResponse(Bill bill, Map<Long, Long> itemCounts) {
        return BillDto.SummaryResponse.builder()
                .id(bill.getId())
                .billNumber(bill.getBillNumber())
                .billDate(bill.getBillDate())
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .dueAmount(bill.getDueAmount())
                .status(bill.getStatus())
                .itemCount(itemCounts.getOrDefault(bill.getId(), 0L).intValue())
                .build();
    }
}