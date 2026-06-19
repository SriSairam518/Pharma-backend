package com.sairam.pharma.dto;

// ================================================================
// AgencyDto.java  —  DATA TRANSFER OBJECTS (DTO)
//
// WHAT IS A DTO?
// A DTO is a simple object used to carry data between layers.
// We NEVER expose the Entity directly to the API.
//
// WHY NOT EXPOSE THE ENTITY DIRECTLY?
// 1. Security: Entity might have sensitive fields you don't want to send
// 2. Flexibility: API shape can change without changing the DB table
// 3. Validation: You put @NotNull etc. on the DTO, not the Entity
//
// ANALOGY: The Entity is your kitchen (internal). The DTO is
// the plate that comes out to the customer (external).
//
// WE HAVE TWO DTOs:
//   AgencyRequest  → what the frontend SENDS (create/update)
//   AgencyResponse → what the backend RETURNS (read)
// ================================================================

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AgencyDto {

    // ============================================================
    // REQUEST DTO  —  frontend sends this when creating/updating
    // ============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        // @NotBlank = not null AND not empty AND not just spaces
        @NotBlank(message = "Agency name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @NotBlank(message = "Contact person name is required")
        @Size(min = 2, max = 100, message = "Contact person name must be between 2 and 100 characters")
        private String contactPerson;

        @NotBlank(message = "Phone number is required")
        // @Pattern validates with a regex — ^ means start, $ means end
        @Pattern(
                regexp = "^[6-9]\\d{9}$",
                message = "Enter a valid 10-digit Indian mobile number"
        )
        private String phone;

        // @Email validates the email format
        @Email(message = "Enter a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        private String email;   // optional — no @NotBlank

        private String address; // optional

        @Pattern(
                regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
                message = "Enter a valid GSTIN (e.g. 29ABCDE1234F1Z5)"
                // ^$ allows empty string (optional field)
        )
        private String gstin;   // optional
    }

    // ============================================================
    // RESPONSE DTO  —  backend sends this to the frontend
    // ============================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {

        private Long   id;
        private String name;
        private String contactPerson;
        private String phone;
        private String email;
        private String address;
        private String gstin;

        // @JsonFormat controls how dates appear in JSON
        // Without this, Java sends an ugly array [2024,1,15,10,30,0]
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;
    }
}