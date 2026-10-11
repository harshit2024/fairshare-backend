package com.fairShare.fairshare_backend.auth.controller;


import com.fairShare.fairshare_backend.auth.dto.*;

import com.fairShare.fairshare_backend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 1. Register a new user
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    // 2. Login
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    // 3. Refresh access token
    @PostMapping("/refreshrefresh")
    public ResponseEntity<?> refresh(
            @Valid @RequestBody RefreshRequest request) {

        return ResponseEntity.ok(authService.refresh(request));
    }

    // 4. Logout
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request) {

        authService.logout(request.refreshToken());

        return ResponseEntity.noContent().build();
    }

    // 5. Request password reset
//    @PostMapping("/forgot-password")
//    public ResponseEntity<Void> forgotPassword(
//            @Valid @RequestBody ForgotPasswordRequest request) {
//
//        authService.forgotPassword(request.email());
//
//        return ResponseEntity.accepted().build();
//    }

    // 6. Reset password using a valid reset token
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(
                request.token(),
                request.newPassword()
        );

        return ResponseEntity.noContent().build();
    }

    // 7. Verify email
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        authService.verifyEmail(request.token());

        return ResponseEntity.noContent().build();
    }

    // 8. Resend email verification
//    @PostMapping("/resend-verification")
//    public ResponseEntity<Void> resendVerification(
//            @Valid @RequestBody ResendVerificationRequest request) {
//
//        authService.resendVerification(request.email());
//
//        return ResponseEntity.accepted().build();
//    }
}
