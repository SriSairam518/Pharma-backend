package com.sairam.pharma.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AgencyDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "Agency name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @NotBlank(message = "Contact person name is required")
        @Size(min = 2, max = 100, message = "Contact person name must be between 2 and 100 characters")
        private String contactPerson;

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[6-9]\\d{9}$",
                message = "Enter a valid 10-digit Indian mobile number"
        )
        private String phone;

        @Email(message = "Enter a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        private String email;   // optional — no @NotBlank

        private String address; // optional

        @Pattern(
                regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
                message = "Enter a valid GSTIN (e.g. 29ABCDE1234F1Z5)"
        )
        private String gstin;   // optional
    }

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

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;
    }
}