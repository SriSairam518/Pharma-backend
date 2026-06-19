package com.sairam.pharma.exception;

// ================================================================
// GlobalExceptionHandler.java  —  CENTRALIZED ERROR HANDLING
//
// WHAT IS THIS?
// Without this, if something goes wrong in your app, Spring sends
// an ugly HTML error page or a confusing stack trace to the frontend.
//
// This class CATCHES all exceptions from any controller and turns
// them into clean, consistent JSON responses.
//
// HOW IT WORKS:
// @RestControllerAdvice  = "watch ALL controllers for exceptions"
// @ExceptionHandler(X)   = "when exception X is thrown, run this method"
//
// The frontend gets:
//   { "success": false, "message": "Agency not found with id: 99" }
// Instead of:
//   500 Internal Server Error with a Java stack trace
// ================================================================

import com.sairam.pharma.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- 400: Bad input (e.g. invalid file type, payment > due amount) ----
    // IllegalArgumentException → used in UploadController for file validation
    // ResponseStatusException  → used in PaymentService for "amount > due" check
    @ExceptionHandler({IllegalArgumentException.class, ResponseStatusException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        String message = ex instanceof ResponseStatusException rse
                ? rse.getReason()
                : ex.getMessage();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    // ---- 413: File too large ----
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("File size exceeds the maximum allowed limit (10MB)"));
    }

    // ---- 404: Resource not found ----
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)           // HTTP 404
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ---- 409: Duplicate data ----
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)            // HTTP 409
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ---- 400: Validation failed (@NotBlank, @Pattern etc. failed) ----
    // This triggers when the DTO validation annotations fail
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        // Collect all field errors into a map: { "phone": "Enter a valid number" }
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String message   = error.getDefaultMessage();
                    errors.put(fieldName, message);
                });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)         // HTTP 400
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    // ---- 500: Catch-all for unexpected errors ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        // Log the real error for debugging (don't send stack trace to client)
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // HTTP 500
                .body(ApiResponse.error("Something went wrong. Please try again."));
    }
}