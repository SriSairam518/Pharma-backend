package com.sairam.pharma.dto;

// ================================================================
// ApiResponse.java  —  STANDARD RESPONSE WRAPPER
//
// WHAT IS THIS?
// Every API endpoint returns this same wrapper object.
// This gives the frontend a consistent, predictable structure.
//
// WITHOUT THIS (inconsistent — bad):
//   Success: { "id": 1, "name": "Sun Pharma" }
//   Error:   { "error": "Not found" }
//
// WITH THIS (consistent — good):
//   Success: { "success": true,  "message": "Created", "data": {...} }
//   Error:   { "success": false, "message": "Not found", "data": null }
//
// The frontend can always check response.success first.
// ================================================================

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// <T> is a GENERIC TYPE — it means "data can be any type"
// ApiResponse<AgencyDto.Response>  → data is one agency
// ApiResponse<List<AgencyDto.Response>> → data is a list
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String  message;
    private T       data;      // any type — the actual payload

    // ---- Static factory methods — convenient shortcuts ----

    // Use when everything worked fine
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // Use when there's no data to return (e.g. after DELETE)
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    // Use when something went wrong
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}