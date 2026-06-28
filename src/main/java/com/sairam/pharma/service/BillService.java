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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
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

        List<Bill> bills = billRepository
                .findByAgencyIdAndBillDateBetweenOrderByBillDateDesc(agencyId, startDate, endDate);

        BigDecimal totalBilled = billRepository.sumTotalAmountByAgencyAndDateRange(agencyId, startDate, endDate);
        BigDecimal totalPaid   = billRepository.sumPaidAmountByAgencyAndDateRange(agencyId, startDate, endDate);
        BigDecimal totalDue    = billRepository.sumDueAmountByAgencyAndDateRange(agencyId, startDate, endDate);

        return BillDto.AgencyBillsSummary.builder()
                .agencyId(agency.getId())
                .agencyName(agency.getName())
                .totalBilledAmount(totalBilled)
                .totalPaidAmount(totalPaid)
                .totalDueAmount(totalDue)
                .billCount(bills.size())
                .bills(bills.stream().map(this::toSummaryResponse).collect(Collectors.toList()))
                .build();
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

        if (request.getBillImageUrl() != null && !request.getBillImageUrl().equals(oldImageUrl)) {
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

        if (request.getBillImageUrl() != null && !request.getBillImageUrl().equals(oldImageUrl)) {
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
                if (publicId != null) {
                    fileStorageService.deleteFile(publicId);
                }
            } catch (Exception e) {
                log.warn("Could not delete Cloudinary image {} after bill {} deletion: {}",
                        imageUrl, id, e.getMessage());
            }
        }

        log.info("Deleted bill {} and {} associated image(s) from Cloudinary",
                id, imageUrlsToDelete.size());
    }

    public static BillStatus calculateStatus(
            BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal dueAmount) {
        if (dueAmount.compareTo(BigDecimal.ZERO) <= 0) return BillStatus.PAID;
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) return BillStatus.UNPAID;
        return BillStatus.PARTIALLY_PAID;
    }

    @Transactional
    public Bill saveBill(Bill bill) {
        return billRepository.save(bill);
    }

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