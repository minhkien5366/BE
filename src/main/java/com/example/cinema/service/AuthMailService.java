package com.example.cinema.service;

public interface AuthMailService {
    /**
     * Gửi mã OTP xác thực khôi phục mật khẩu tài khoản
     *
     * @param toEmail Địa chỉ email nhận mã
     * @param otpCode Mã OTP ngẫu nhiên được sinh ra từ hệ thống
     */
    void sendOtpEmail(String toEmail, String otpCode);
}