package com.example.cinema.service;

import com.example.cinema.dto.JwtResponse;
import com.example.cinema.dto.LoginRequest;
import com.example.cinema.dto.RegisterRequest;
import com.example.cinema.dto.ResetPasswordRequest; // Import DTO mới

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);
    String register(RegisterRequest registerRequest);

    // 🎯 KHAI BÁO 2 HÀM MỚI Ở ĐÂY
    String forgotPassword(String email);
    String resetPassword(ResetPasswordRequest request);
}