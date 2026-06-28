package com.sairam.pharma.controller;

// ================================================================
// AuthController.java  —  AUTH ENDPOINTS
//
// POST /api/auth/login            → returns JWT token
// POST /api/auth/forgot-password  → sends reset email
// POST /api/auth/reset-password   → updates password with token
//
// All three are PUBLIC — they're in the permitAll() list in
// SecurityConfig so no JWT is needed to reach them (which makes
// sense since login is how you GET the JWT in the first place ).
// ================================================================

import com.sairam.pharma.dto.ApiResponse;
import com.sairam.pharma.dto.AuthDto;
import com.sairam.pharma.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request
    ) {
        AuthDto.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "If that email is registered, a reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody AuthDto.ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password updated successfully. You can now log in."
        ));
    }
}