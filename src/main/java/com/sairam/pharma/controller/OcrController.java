package com.sairam.pharma.controller;

// ================================================================
// OcrController.java  —  CONTROLLER
//
// ENDPOINT:
//   POST /api/ocr/scan
//   Body: { "imageUrl": "/uploads/bills/abc.jpg" }
//   Returns: list of extracted medicine rows
//
// FLOW:
//   1. React uploads bill image → gets back imageUrl
//   2. React sends imageUrl here
//   3. We read the file from disk, send to Google Vision
//   4. Return parsed medicine rows to React
//   5. React shows editable table pre-filled with these rows
// ================================================================

import com.sairam.pharma.dto.ApiResponse;
import com.sairam.pharma.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Slf4j
public class OcrController {

    private final OcrService ocrService;

    @GetMapping("/scan")
    public String getScan(){
        return "scanning...";
    }

    // POST /api/ocr/scan
    // Body:    { "imageUrl": "/uploads/bills/abc.jpg" }
    // Returns: {
    //   billNumber, billDate, vendorTotal, calculatedTotal,
    //   totalMismatch, totalNote, items: [...]
    // }
    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scanBill(
            @RequestBody Map<String, String> body
    ) {
        String imageUrl = body.get("imageUrl");
        if (imageUrl == null || imageUrl.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("imageUrl is required"));
        }

        Map<String, Object> result = ocrService.extractBillData(imageUrl);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

        String billNo = (String) result.get("billNumber");
        String message = (items == null || items.isEmpty())
                ? "No medicine items found — please fill manually"
                : "Extracted " + items.size() + " items" +
                  (billNo != null ? " · Bill #" + billNo : "");

        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}