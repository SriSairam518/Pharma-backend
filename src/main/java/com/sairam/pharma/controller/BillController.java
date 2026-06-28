package com.sairam.pharma.controller;

// ================================================================
// BillController.java  —  CONTROLLER
//
// ENDPOINTS:
//   POST   /api/bills                    → create a new bill
//   GET    /api/bills/{id}                → get one bill (full detail)
//   PUT    /api/bills/{id}                → update a bill
//   DELETE /api/bills/{id}                → delete a bill
//   GET    /api/agencies/{agencyId}/bills → bills for one agency (filtered)
// ================================================================

import com.sairam.pharma.dto.ApiResponse;
import com.sairam.pharma.dto.BillDto;
import com.sairam.pharma.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @PostMapping("/api/bills")
    public ResponseEntity<ApiResponse<BillDto.Response>> createBill(
            @Valid @RequestBody BillDto.Request request
    ) {
        BillDto.Response created = billService.createBill(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bill created successfully", created));
    }

    @GetMapping("/api/bills/{id}")
    public ResponseEntity<ApiResponse<BillDto.Response>> getBillById(
            @PathVariable Long id
    ) {
        BillDto.Response bill = billService.getBillById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Bill fetched successfully", bill)
        );
    }

    @PutMapping("/api/bills/{id}")
    public ResponseEntity<ApiResponse<BillDto.Response>> updateBill(
            @PathVariable Long id,
            @Valid @RequestBody BillDto.Request request
    ) {
        BillDto.Response updated = billService.updateBill(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Bill updated successfully", updated)
        );
    }

    @DeleteMapping("/api/bills/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBill(
            @PathVariable Long id
    ) {
        billService.deleteBill(id);
        return ResponseEntity.ok(
                ApiResponse.success("Bill deleted successfully")
        );
    }

    @GetMapping("/api/agencies/{agencyId}/bills")
    public ResponseEntity<ApiResponse<BillDto.AgencyBillsSummary>> getAgencyBills(
            @PathVariable Long agencyId,
            @RequestParam(required = false) Integer days,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        BillDto.AgencyBillsSummary summary = billService.getAgencyBills(agencyId, days, from, to);
        return ResponseEntity.ok(
                ApiResponse.success("Bills fetched successfully", summary)
        );
    }
}
