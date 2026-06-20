package com.sairam.pharma.service;

// ================================================================
// OcrService.java  —  SERVICE
//
// WHAT DOES THIS DO?
// Sends a bill image to Google Cloud Vision API → gets back raw text
// → parses the text into structured medicine rows.
//
// HOW GOOGLE VISION OCR WORKS:
// 1. We send the image as Base64 (text encoding of binary data)
// 2. Google reads every word on the image (even handwritten text)
// 3. Returns all text as one big string with line breaks
// 4. We parse that string to find medicine rows
//
// WHY BASE64?
// HTTP/JSON can only carry text. We convert the image file to
// Base64 so it can travel inside a JSON request body.
//
// PARSING STRATEGY:
// Medical bills in India typically have this format per line:
//   "Paracetamol 500mg  PCT2024A  Dec-26  100  2.50  250.00"
//   Name  |  Batch  |  Expiry  |  Qty  |  Price  |  Total
//
// We use regex patterns to detect numbers and dates in each line
// and split them into the right fields.
// ================================================================

// ================================================================
// OcrService.java  —  MISTRAL AI OCR
//
// HOW THIS WORKS:
// 1. Read the uploaded bill image from disk
// 2. Convert it to Base64 (so it can travel inside JSON)
// 3. Send it to Mistral's vision model with a detailed prompt
// 4. Mistral reads the image AND understands it — returns JSON
// 5. We parse that JSON into medicine rows
//
// WHY MISTRAL IS BETTER THAN TESSERACT FOR MEDICINE BILLS:
// Tesseract = reads characters blindly (if blur → wrong letter)
// Mistral   = understands context ("this looks like a medicine
//             name even if one letter is smudged")
//
// MISTRAL MODEL USED: pixtral-12b-2409
// This is Mistral's vision model — it can see and understand images.
// It is available on the free tier.
// ================================================================

// ================================================================
// OcrService.java  —  MISTRAL AI OCR
// UPDATED to scan real pharma invoice columns:
// HSN | Product Name | Pack | Batch | Expiry | Qty | MRP | Rate | Disc | GST | Amount
// Plus bill-level: Sub Total, Discount, GST, Net Amount
//
// QUANTITY HANDLING:
// Bills often show "10 + 5" (paid + free) or "4.5 + 0.5".
// We instruct Mistral to ADD these together and return ONE
// combined number (e.g. "10 + 5" → 15).
//
// EVERYTHING IS SCANNED — NOTHING CALCULATED.
// Amount, MRP, Rate, Discount, GST, Sub Total, Net Amount are all
// read directly from the image. We never compute qty × rate etc.
// ================================================================

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class OcrService {

    @Value("${mistral.api.key}")
    private String apiKey;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90,    java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30,   java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mistral.api.url}")
    private String MISTRAL_API_URL;

    public Map<String, Object> extractBillData(String imageFilePath) {

        Request request  = new Request.Builder()
                .url(imageFilePath)
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            byte[] imageBytes = response.body().bytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType    = detectMimeType(imageFilePath);

            String responseText = callMistralApi(base64Image, mimeType);

            if (responseText == null || responseText.isBlank()) {
                log.warn("Mistral returned empty response for: {}", imageFilePath);
                return emptyResult();
            }

            log.info("Mistral response received ({} chars)", responseText.length());
            return parseMistralResponse(responseText);

        } catch (IOException e) {
            log.error("OCR failed for {}: {}", imageFilePath, e.getMessage());
            throw new RuntimeException("OCR processing failed: " + e.getMessage());
        }
    }

    private String callMistralApi(String base64Image, String mimeType) throws IOException {

        String prompt = """
            You are an expert at reading Indian pharmacy purchase invoices.

            Look at this bill image carefully and extract ALL information.
            This bill has a table with columns similar to:
            HSN | Product Name | Pack | Batch No | Expiry | Qty | MRP | Rate | Disc | GST | Amount

            Return ONLY a single valid JSON object. No explanation. No markdown. No code blocks.
            Just raw JSON starting with { and ending with }.

            The JSON must have this exact structure:
            {
              "billNumber": "the invoice/bill number, or null",
              "billDate": "bill date in YYYY-MM-DD format, or null",
              "subTotal": gross/sub total amount before discount and GST, as a number, or null,
              "billDiscount": total discount amount on the whole bill, as a number, or null,
              "billGst": total GST amount on the whole bill, as a number, or null,
              "netAmount": the final NET AMOUNT or GRAND TOTAL printed on the bill, as a number, or null,
              "items": [
                {
                  "hsnCode": "HSN code if shown, or null",
                  "medicineName": "full product/medicine name with strength",
                  "pack": "pack size as printed e.g. '10*10' or '1*100ML', or null",
                  "batchNumber": "batch/lot number, or null",
                  "expiryDate": "expiry in YYYY-MM-DD format using last day of the month, or null",
                  "quantity": combined quantity as a number,
                  "mrp": MRP as printed, as a number, or null,
                  "rate": rate/purchase price as printed, as a number, or null,
                  "discount": discount as printed (keep exact text like '5%' or '10'), or null,
                  "gst": GST as printed (keep exact text like '12%' or '15.50'), or null,
                  "amount": the final line AMOUNT as printed for this row, as a number, or null
                }
              ]
            }

            CRITICAL — Quantity handling:
            Many bills show quantity as "paid + free", such as "10 + 5" or "4.5 + 0.5".
            When you see this format, ADD the two numbers together and return ONE
            combined number. Example: "10 + 5" becomes 15. "4.5 + 0.5" becomes 5.
            If there is a separate "FREE" or "F" column showing free quantity
            alongside a regular quantity column, add that free quantity to the
            main quantity too and return the combined total.

            CRITICAL — Do not calculate anything:
            - amount: read the printed line total exactly as shown, do NOT compute qty x rate
            - netAmount: read the printed grand total exactly, do NOT sum the items yourself
            - subTotal, billDiscount, billGst: read exactly as printed in the bill's summary section
            - discount and gst per item: keep them as text exactly as printed (with % sign if shown)

            Other rules:
            - billNumber: look for Invoice No, Bill No, Inv#, Receipt No
            - billDate: look for Date, Invoice Date, Bill Date — convert to YYYY-MM-DD
            - expiryDate: "12/26" -> "2026-12-31", "Mar-27" -> "2027-03-31"
            - Do NOT include header rows or the totals row inside "items"
            - If a field is not visible on the bill, use null for that field — do not guess
            - Include EVERY medicine row visible on the bill

            Return only the JSON object, nothing else.
            """;

        String requestBody = """
            {
              "model": "pixtral-12b-2409",
              "messages": [
                {
                  "role": "user",
                  "content": [
                    { "type": "text", "text": %s },
                    { "type": "image_url", "image_url": { "url": "data:%s;base64,%s" } }
                  ]
                }
              ],
              "max_tokens": 4000,
              "temperature": 0.1
            }
            """.formatted(objectMapper.writeValueAsString(prompt), mimeType, base64Image);

        Request request = new Request.Builder()
                .url(MISTRAL_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Mistral API error {}: {}", response.code(), body);
                throw new RuntimeException("Mistral API error " + response.code() + ": " + extractErrorMessage(body));
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode content = root.path("choices").get(0).path("message").path("content");
            return content.isMissingNode() ? null : content.asText();
        }
    }

    private Map<String, Object> parseMistralResponse(String responseText) {
        try {
            String cleaned = responseText.trim()
                    .replaceAll("^```[a-z]*\\n?", "")
                    .replaceAll("```$", "")
                    .trim();

            int start = cleaned.indexOf('{');
            int end   = cleaned.lastIndexOf('}');
            if (start == -1 || end == -1 || end <= start) {
                log.warn("No JSON object found in Mistral response");
                return emptyResult();
            }

            JsonNode root = objectMapper.readTree(cleaned.substring(start, end + 1));

            String billNumber   = getTextOrNull(root, "billNumber");
            String billDate     = getTextOrNull(root, "billDate");
            Double subTotal     = getDoubleOrNull(root, "subTotal");
            Double billDiscount = getDoubleOrNull(root, "billDiscount");
            Double billGst      = getDoubleOrNull(root, "billGst");
            Double netAmount    = getDoubleOrNull(root, "netAmount");

            JsonNode itemsNode = root.path("items");
            List<Map<String, Object>> items = new ArrayList<>();

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String name = getTextOrNull(item, "medicineName");
                    if (name == null || name.isBlank()) continue;

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("hsnCode",      getTextOrNull(item, "hsnCode"));
                    row.put("medicineName", name.trim());
                    row.put("pack",         getTextOrNull(item, "pack"));
                    row.put("batchNumber",  getTextOrNull(item, "batchNumber"));
                    row.put("expiryDate",   getTextOrNull(item, "expiryDate"));
                    row.put("quantity",     getDoubleOrNull(item, "quantity"));
                    row.put("mrp",          getDoubleOrNull(item, "mrp"));
                    row.put("rate",         getDoubleOrNull(item, "rate"));
                    row.put("discount",     getTextOrNull(item, "discount"));
                    row.put("gst",          getTextOrNull(item, "gst"));
                    row.put("amount",       getDoubleOrNull(item, "amount"));

                    items.add(row);
                }
            }

            log.info("OCR result: billNo={}, date={}, {} items, netAmount={}",
                    billNumber, billDate, items.size(), netAmount);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("billNumber",   billNumber);
            result.put("billDate",     billDate);
            result.put("subTotal",     subTotal);
            result.put("billDiscount", billDiscount);
            result.put("billGst",      billGst);
            result.put("netAmount",    netAmount);
            result.put("items",        items);
            return result;

        } catch (Exception e) {
            log.error("Failed to parse Mistral response: {}", e.getMessage());
            return emptyResult();
        }
    }

    private Map<String, Object> emptyResult() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("billNumber",   null);
        result.put("billDate",     null);
        result.put("subTotal",     null);
        result.put("billDiscount", null);
        result.put("billGst",      null);
        result.put("netAmount",    null);
        result.put("items",        Collections.emptyList());
        return result;
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        String text = v.asText().trim();
        return text.equalsIgnoreCase("null") || text.isBlank() ? null : text;
    }

    private Double getDoubleOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        try {
            // Handle case where Mistral returns a number-as-string despite instructions
            if (v.isTextual()) {
                String cleaned = v.asText().replaceAll("[^0-9.\\-]", "");
                return cleaned.isBlank() ? null : Double.parseDouble(cleaned);
            }
            return v.asDouble();
        } catch (Exception e) {
            return null;
        }
    }

    private String detectMimeType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private String extractErrorMessage(String errorBody) {
        try {
            JsonNode root = objectMapper.readTree(errorBody);
            JsonNode msg  = root.path("message");
            return msg.isMissingNode() ? errorBody : msg.asText();
        } catch (Exception e) {
            return errorBody;
        }
    }
}