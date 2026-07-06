package com.example.cinema.service.impl;

import com.example.cinema.service.AuthMailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthMailServiceImpl implements AuthMailService {

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 🔥 ĐÃ THÊM: Chỉ định chính xác email người gửi và Tên hiển thị (Alias)
            helper.setFrom("kienphatanh@gmail.com", "A&K Cinema Security");            
            helper.setTo(toEmail);
            helper.setSubject("A&K CINEMA - MÃ XÁC THỰC QUÊN MẬT KHẨU");

            StringBuilder content = new StringBuilder();
            content.append("<div style='background-color: #050507; padding: 40px 15px; font-family: system-ui, -apple-system, sans-serif;'>");
            content.append("<div style='max-width: 480px; margin: 0 auto; background-color: #0b0b0f; border: 1px solid rgba(220,38,38,0.15); border-radius: 32px; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.7);'>");
            
            // --- HEADER EMAIL ---
            content.append("<div style='background: linear-gradient(135deg, #111, #1e050a); padding: 35px 25px; text-align: center; border-bottom: 2px dashed #050507;'>");
            content.append("<div style='font-size: 10px; font-weight: 900; letter-spacing: 5px; color: #dc2626; text-transform: uppercase; margin-bottom: 8px;'>A&K CINEMA SECURITY</div>");
            content.append("<h2 style='margin: 0; font-size: 24px; font-weight: 900; color: #ffffff; text-transform: uppercase; letter-spacing: -0.5px;'>Khôi Phục Mật Khẩu</h2>");
            content.append("</div>");

            // --- THÂN EMAIL CHỨA MÃ OTP ---
            content.append("<div style='padding: 40px 25px; background-color: #0b0b0f; text-align: center;'>");
            content.append("<p style='color: #9ca3af; font-size: 14px; margin-bottom: 30px; line-height: 1.6;'>Chúng tôi nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn tại hệ thống <strong>A&K Cinema</strong>. Dưới đây là mã xác thực (OTP) của bạn:</p>");
            
            // Khối hiển thị mã OTP nổi bật dạng Neon Glow
            content.append("<div style='display: inline-block; background-color: #18181b; border: 2px solid #dc2626; border-radius: 16px; padding: 15px 40px; margin-bottom: 30px; box-shadow: 0 0 20px rgba(220,38,38,0.2);'>");
            content.append("<span style='font-size: 36px; font-weight: 900; color: #ffffff; letter-spacing: 12px; margin-right: -12px;'>").append(otpCode).append("</span>");
            content.append("</div>");

            content.append("<p style='color: #ef4444; font-size: 12px; font-weight: bold; margin-bottom: 10px;'>Mã xác thực này sẽ hết hạn trong vòng 5 phút.</p>");
            content.append("<p style='color: #52525b; font-size: 11px;'>If you did not request a password reset, please ignore this email. Tuyệt đối không chia sẻ mã OTP này cho bất kỳ ai khác.</p>");
            content.append("</div>");

            // --- FOOTER ---
            content.append("<div style='background-color: #060608; padding: 20px; text-align: center; color: #3f3f46; font-size: 10px; font-weight: bold;'>");
            content.append("<p style='margin: 0 0 4px 0;'>HỆ THỐNG BẢO MẬT TÀI KHOẢN A&K CINEMA</p>");
            content.append("<p style='margin: 0;'>© 2026 A&K Cinema. All rights reserved.</p>");
            content.append("</div>");
            
            content.append("</div>");
            content.append("</div>");

            helper.setText(content.toString(), true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail OTP khôi phục mật khẩu: " + e.getMessage());
        }
    }
}