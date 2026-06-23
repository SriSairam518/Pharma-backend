package com.sairam.pharma.entity;

// ================================================================
// AdminUser.java  —  SINGLE ROW, SINGLE TABLE, FOREVER
//
// DESIGN DECISIONS for minimal storage:
//
// 1. No separate password_reset_tokens table.
//    Reset token lives here as two nullable columns.
//    When a reset is requested  → write token + expiry onto this row.
//    When reset completes       → clear both columns (set to null).
//    One row, always. Zero extra tables.
//
// 2. No audit/created_at columns beyond updatedAt.
//    We only need updatedAt to know when password last changed.
//
// 3. Token + expiry are nullable — they only have values while a
//    reset is in progress (< 15 minutes). After that they're null.
// ================================================================

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "admin_user")
@EntityListeners(AuditingEntityListener.class)
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // BCrypt hash — never the plain text password
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // Recovery email — only used to send the reset link
    @Column(nullable = false, length = 150)
    private String email;

    // ---- RESET TOKEN FIELDS ----
    // Null when no reset is in progress.
    // Set when "Forgot password" is triggered.
    // Cleared immediately after the reset is used.
    // No second table needed — just these two nullable columns.

    @Column(name = "reset_token", length = 100, unique = true)
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    // When was the password last changed — useful audit info
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}