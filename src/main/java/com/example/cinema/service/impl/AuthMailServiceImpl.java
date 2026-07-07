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

            helper.setFrom("kienphatanh@gmail.com", "KN Cinema Security");
            helper.setTo(toEmail);
            helper.setSubject("KN CINEMA - MÃ XÁC THỰC KHÔI PHỤC MẬT KHẨU");

            String safeOtp = escapeHtml(otpCode);

            StringBuilder content = new StringBuilder();

            content.append("<div style='margin:0;padding:0;background:#070b14;font-family:Arial,Helvetica,sans-serif;color:#f8fafc;'>");

            content.append("<div style='width:100%;padding:42px 14px;background:")
                    .append("radial-gradient(circle at top center,rgba(103,232,249,0.12),transparent 34%),")
                    .append("radial-gradient(circle at bottom left,rgba(244,212,25,0.10),transparent 32%),")
                    .append("#070b14;'>");

            content.append("<div style='max-width:520px;margin:0 auto;background:#0b1020;border:1px solid rgba(255,255,255,0.10);border-radius:28px;overflow:hidden;box-shadow:0 30px 80px rgba(0,0,0,0.55);'>");

            // HEADER
            content.append("<div style='padding:34px 28px 28px 28px;text-align:center;background:linear-gradient(135deg,#0b1020,#111827);border-bottom:1px solid rgba(255,255,255,0.10);'>");

            content.append("<div style='display:inline-block;padding:10px 14px;border-radius:16px;background:rgba(244,212,25,0.10);border:1px solid rgba(244,212,25,0.28);margin-bottom:18px;'>");
            content.append("<span style='display:inline-block;font-size:30px;line-height:1;font-weight:900;letter-spacing:-2px;color:#f4d419;'>KN</span>");
            content.append("<span style='display:inline-block;margin-left:6px;font-size:11px;font-weight:900;letter-spacing:4px;text-transform:uppercase;color:#f8fafc;vertical-align:middle;'>Cinema</span>");
            content.append("</div>");

            content.append("<div style='font-size:10px;font-weight:900;letter-spacing:4px;color:#67e8f9;text-transform:uppercase;margin-bottom:10px;'>Account Security Center</div>");
            content.append("<h1 style='margin:0;color:#ffffff;font-size:26px;line-height:1.15;font-weight:900;text-transform:uppercase;letter-spacing:-0.8px;'>Khôi phục mật khẩu</h1>");
            content.append("<p style='margin:12px 0 0 0;color:#64748b;font-size:12px;font-weight:700;letter-spacing:1px;text-transform:uppercase;'>Mã xác thực một lần</p>");
            content.append("</div>");

            // BODY
            content.append("<div style='padding:34px 28px 30px 28px;text-align:center;background:#0b1020;'>");

            content.append("<p style='margin:0 auto 26px auto;max-width:420px;color:#cbd5e1;font-size:14px;line-height:1.75;font-weight:500;'>");
            content.append("Chúng tôi nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn tại hệ thống ");
            content.append("<strong style='color:#f4d419;font-weight:900;'>KN Cinema</strong>. ");
            content.append("Vui lòng sử dụng mã OTP bên dưới để tiếp tục xác thực.");
            content.append("</p>");

            content.append("<div style='display:inline-block;margin:0 auto 26px auto;padding:18px 26px;border-radius:20px;background:#080c1b;border:1px solid rgba(244,212,25,0.45);box-shadow:0 0 0 6px rgba(244,212,25,0.06),0 24px 48px rgba(0,0,0,0.32);'>");
            content.append("<div style='font-size:11px;font-weight:900;letter-spacing:3px;color:#67e8f9;text-transform:uppercase;margin-bottom:8px;'>Mã OTP của bạn</div>");
            content.append("<div style='font-size:40px;line-height:1;font-weight:900;letter-spacing:12px;color:#f4d419;margin-right:-12px;'>")
                    .append(safeOtp)
                    .append("</div>");
            content.append("</div>");

            content.append("<div style='max-width:420px;margin:0 auto;padding:15px 16px;border-radius:16px;background:rgba(251,191,36,0.08);border:1px solid rgba(244,212,25,0.18);'>");
            content.append("<p style='margin:0;color:#fde68a;font-size:12px;line-height:1.6;font-weight:800;'>Mã xác thực này sẽ hết hạn trong vòng 5 phút.</p>");
            content.append("</div>");

            content.append("<p style='margin:22px auto 0 auto;max-width:420px;color:#64748b;font-size:11px;line-height:1.7;font-weight:600;'>");
            content.append("Nếu bạn không yêu cầu khôi phục mật khẩu, hãy bỏ qua email này. Tuyệt đối không chia sẻ mã OTP cho bất kỳ ai, kể cả nhân viên hỗ trợ.");
            content.append("</p>");

            content.append("</div>");

            // SECURITY STRIP
            content.append("<div style='padding:18px 28px;background:#080c1b;border-top:1px solid rgba(255,255,255,0.08);border-bottom:1px solid rgba(255,255,255,0.08);'>");
            content.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;'>");
            content.append("<tr>");
            content.append("<td style='width:33.33%;text-align:center;padding:4px;'>");
            content.append("<div style='font-size:9px;font-weight:900;color:#64748b;text-transform:uppercase;letter-spacing:1.5px;'>Bảo mật</div>");
            content.append("<div style='font-size:11px;font-weight:900;color:#67e8f9;margin-top:4px;'>OTP VERIFY</div>");
            content.append("</td>");
            content.append("<td style='width:33.33%;text-align:center;padding:4px;border-left:1px solid rgba(255,255,255,0.08);border-right:1px solid rgba(255,255,255,0.08);'>");
            content.append("<div style='font-size:9px;font-weight:900;color:#64748b;text-transform:uppercase;letter-spacing:1.5px;'>Hiệu lực</div>");
            content.append("<div style='font-size:11px;font-weight:900;color:#f4d419;margin-top:4px;'>5 PHÚT</div>");
            content.append("</td>");
            content.append("<td style='width:33.33%;text-align:center;padding:4px;'>");
            content.append("<div style='font-size:9px;font-weight:900;color:#64748b;text-transform:uppercase;letter-spacing:1.5px;'>Hệ thống</div>");
            content.append("<div style='font-size:11px;font-weight:900;color:#f8fafc;margin-top:4px;'>KN CINEMA</div>");
            content.append("</td>");
            content.append("</tr>");
            content.append("</table>");
            content.append("</div>");

            // FOOTER
            content.append("<div style='padding:22px 24px;text-align:center;background:#0b1020;'>");
            content.append("<p style='margin:0 0 6px 0;color:#475569;font-size:10px;font-weight:900;letter-spacing:2px;text-transform:uppercase;'>Hệ thống bảo mật tài khoản KN Cinema</p>");
            content.append("<p style='margin:0;color:#334155;font-size:10px;font-weight:700;'>© 2026 KN Cinema. All rights reserved.</p>");
            content.append("</div>");

            content.append("</div>");
            content.append("</div>");
            content.append("</div>");

            helper.setText(content.toString(), true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail OTP khôi phục mật khẩu: " + e.getMessage());
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}