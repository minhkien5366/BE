package com.example.cinema.controller;

import com.example.cinema.dto.*;
import com.example.cinema.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        return ResponseEntity.ok(
                ApiResponse.<JwtResponse>builder()
                        .status(200)
                        .message("Đăng nhập thành công")
                        .data(authService.login(loginRequest))
                        .build()
        );
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        String message = authService.register(registerRequest);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(200)
                        .message(message)
                        .data(null)
                        .build()
        );
    }

    // ================= FORGOT PASSWORD (FIXED) =================
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        String message = authService.forgotPassword(request.getEmail());

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(200)
                .message(message)
                .data(null)
                .build());
    }

    // ================= RESET PASSWORD =================
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        String message = authService.resetPassword(request);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(200)
                .message(message)
                .data(null)
                .build());
    }
}