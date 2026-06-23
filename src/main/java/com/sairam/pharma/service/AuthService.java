package com.sairam.pharma.service;

// ================================================================
// AuthService.java  —  PRODUCTION-READY, MINIMAL STORAGE
//
// STORAGE STRATEGY:
//   - No separate tokens table. Token lives on the admin_user row.
//   - Forgot password  → write token + expiry onto the row (UPDATE)
//   - Reset password   → verify token, update password, clear token
//                        (everything in one UPDATE — no extra rows)
//   - Result: admin_user is always exactly 1 row, forever.
//
// SECURITY:
//   - Same error message for wrong username AND wrong password
//     (prevents username enumeration)
//   - Forgot password always returns 200 even if email not found
//     (prevents email enumeration)
//   - Token is cleared immediately after use (single-use enforced)
//   - Token expires after 15 minutes
//   - No sensitive data in any log statement
// ================================================================

import com.sairam.pharma.dto.AuthDto;
import com.sairam.pharma.entity.AdminUser;
import com.sairam.pharma.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder     passwordEncoder;
    private final EmailService        emailService;
    private final com.sairam.pharma.security.JwtUtil jwtUtil;

    // ================================================================
    // LOGIN
    // ================================================================
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {

        // Find account — use the same generic error for both
        // "username not found" and "wrong password" cases so an
        // attacker cannot tell which one is true
        AdminUser admin = adminUserRepository
                .findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        log.info("Login successful");

        return AuthDto.LoginResponse.builder()
                .token(jwtUtil.generateToken(admin.getUsername()))
                .username(admin.getUsername())
                .build();
    }

    // ================================================================
    // FORGOT PASSWORD
    // Writes a reset token directly onto the admin_user row (UPDATE).
    // No new rows created anywhere.
    // ================================================================
    @Transactional
    public void forgotPassword(String email) {

        Optional<AdminUser> adminOpt = adminUserRepository
                .findByEmail(email.trim().toLowerCase());

        // Always return success — prevents email enumeration attacks
        if (adminOpt.isEmpty()) {
            log.info("Reset requested for unrecognised account");
            return;
        }

        AdminUser admin      = adminOpt.get();
        String    resetToken = UUID.randomUUID().toString();

        // Write token + expiry onto the existing row (single UPDATE)
        // If there was a previous unused token, this overwrites it —
        // only the latest reset link is ever valid
        admin.setResetToken(resetToken);
        admin.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
        adminUserRepository.save(admin);

        try {
            emailService.sendPasswordResetEmail(admin.getEmail(), resetToken);
        } catch (Exception e) {
            // Clear the token if email failed — don't leave a dangling token
            admin.setResetToken(null);
            admin.setResetTokenExpiresAt(null);
            adminUserRepository.save(admin);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not send reset email. Please try again.");
        }
    }

    // ================================================================
    // RESET PASSWORD
    // Finds admin by token, validates it, updates password,
    // then CLEARS the token columns — all in one transaction.
    // No rows created or deleted — only one UPDATE to the existing row.
    // ================================================================
    @Transactional
    public void resetPassword(AuthDto.ResetPasswordRequest request) {

        // Find the admin whose reset token matches
        AdminUser admin = adminUserRepository
                .findByResetToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));

        // Check expiry
        if (admin.getResetTokenExpiresAt() == null
                || LocalDateTime.now().isAfter(admin.getResetTokenExpiresAt())) {
            // Clear expired token to keep the row clean
            admin.setResetToken(null);
            admin.setResetTokenExpiresAt(null);
            adminUserRepository.save(admin);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This reset link has expired (valid for 15 minutes). Please request a new one.");
        }

        // Update password + clear token in a single save (one DB write)
        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        admin.setResetToken(null);            // token used — clear it immediately
        admin.setResetTokenExpiresAt(null);   // clear expiry too
        adminUserRepository.save(admin);

        log.info("Password reset completed");
    }
}