package com.sairam.pharma.controller;

import com.sairam.pharma.dto.ApiResponse;
import com.sairam.pharma.dto.PaymentDto;
import com.sairam.pharma.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/api/bills/{billId}/payments")
    public ResponseEntity<ApiResponse<PaymentDto.Response>> payBill(
            @PathVariable Long billId,
            @Valid @RequestBody PaymentDto.Request request
    ) {
        PaymentDto.Response payment = paymentService.payBill(billId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully", payment));
    }

    @GetMapping("/api/bills/{billId}/payments")
    public ResponseEntity<ApiResponse<List<PaymentDto.Response>>> getPayments(
            @PathVariable Long billId
    ) {
        List<PaymentDto.Response> payments = paymentService.getPaymentsForBill(billId);
        return ResponseEntity.ok(
                ApiResponse.success("Payment history fetched successfully", payments)
        );
    }
}