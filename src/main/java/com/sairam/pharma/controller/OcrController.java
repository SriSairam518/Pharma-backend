package com.sairam.pharma.controller;

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

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scanBill(
            @RequestBody Map<String, String> body
    ) {
        String imageUrl = body.get("imageUrl");
        if (imageUrl == null || imageUrl.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("imageUrl is required"));
        }
//        log.info("before extract bill data");

        Map<String, Object> result = ocrService.extractBillData(imageUrl);

//        log.info("after extract bill data");

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