package com.sairam.pharma.service;

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

    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {

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

    @Transactional
    public void forgotPassword(String email) {

        Optional<AdminUser> adminOpt = adminUserRepository
                .findByEmail(email.trim().toLowerCase());

        if (adminOpt.isEmpty()) {
            log.info("Reset requested for unrecognised account");
            return;
        }

        AdminUser admin      = adminOpt.get();
        String    resetToken = UUID.randomUUID().toString();

        admin.setResetToken(resetToken);
        admin.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
        adminUserRepository.save(admin);

        try {
            emailService.sendPasswordResetEmail(admin.getEmail(), resetToken);
        } catch (Exception e) {
            admin.setResetToken(null);
            admin.setResetTokenExpiresAt(null);
            adminUserRepository.save(admin);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not send reset email. Please try again.");
        }
    }

    @Transactional
    public void resetPassword(AuthDto.ResetPasswordRequest request) {

        AdminUser admin = adminUserRepository
                .findByResetToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));

        if (admin.getResetTokenExpiresAt() == null
                || LocalDateTime.now().isAfter(admin.getResetTokenExpiresAt())) {
            admin.setResetToken(null);
            admin.setResetTokenExpiresAt(null);
            adminUserRepository.save(admin);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This reset link has expired (valid for 15 minutes). Please request a new one.");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        admin.setResetToken(null);
        admin.setResetTokenExpiresAt(null);
        adminUserRepository.save(admin);

        log.info("Password reset completed");
    }
}