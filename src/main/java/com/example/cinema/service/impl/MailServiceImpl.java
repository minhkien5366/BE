package com.example.cinema.service.impl;

import com.example.cinema.entity.Order;
import com.example.cinema.entity.OrderDetail;
import com.example.cinema.entity.Ticket;
import com.example.cinema.entity.Showtime;
import com.example.cinema.entity.User;
import com.example.cinema.service.MailService;
import com.example.cinema.repository.TicketRepository;
import com.example.cinema.repository.ComboRepository;
import com.example.cinema.entity.Combo;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TicketRepository ticketRepository;
    private final ComboRepository comboRepository;

    private String escape(Object raw) {
        if (raw == null) return "";

        return String.valueOf(raw)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String textOr(Object raw, String fallback) {
        String value = raw == null ? "" : String.valueOf(raw).trim();
        return value.isEmpty() ? fallback : value;
    }

    private String getCustomerName(User user) {
        if (user == null) return "Quý khách";

        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();

        return fullName.isEmpty() ? "Quý khách" : fullName;
    }

    private String formatMoney(NumberFormat formatter, Object amount) {
        try {
            return formatter.format(amount == null ? 0 : amount);
        } catch (Exception e) {
            return "0 ₫";
        }
    }

    private String formatShowDate(Showtime showtime, DateTimeFormatter dateFormatter) {
        try {
            if (showtime == null || showtime.getStartTime() == null) return "N/A";
            return showtime.getStartTime().format(dateFormatter);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String formatShowTime(Showtime showtime, DateTimeFormatter timeFormatter) {
        try {
            if (showtime == null || showtime.getStartTime() == null) return "N/A";

            String start = showtime.getStartTime().format(timeFormatter);
            String end = showtime.getEndTime() != null
                    ? showtime.getEndTime().format(timeFormatter)
                    : "";

            return end.isEmpty() ? start : start + " - " + end;
        } catch (Exception e) {
            return "N/A";
        }
    }

    @Override
    @Async
    public void sendOrderConfirmation(Order order) {
        try {
            if (order == null || order.getUser() == null || order.getUser().getEmail() == null) {
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("kienphatanh@gmail.com", "KN Cinema Ticket");
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("KN CINEMA - VÉ ĐIỆN TỬ XÁC NHẬN #" + order.getId());

            NumberFormat vnFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            String totalAmountFormatted = formatMoney(vnFormat, order.getTotalAmount());
            String customerName = getCustomerName(order.getUser());

            String movieTitle = "Vé Xem Phim";
            String roomName = "N/A";
            String cinemaName = "KN Cinema";
            String showDate = "N/A";
            String showTime = "N/A";
            List<String> seatNames = new ArrayList<>();
            List<String> comboDetails = new ArrayList<>();
            String commonBookingCode = "KN-CINEMA";

            Long detectedShowtimeId = null;

            if (order.getOrderDetails() != null) {
                for (OrderDetail orderDetail : order.getOrderDetails()) {
                    if ("TICKET".equalsIgnoreCase(String.valueOf(orderDetail.getItemType()))) {
                        detectedShowtimeId = ticketRepository.findAll().stream()
                                .filter(ticket ->
                                        ticket.getSeat() != null &&
                                                ticket.getSeat().getId().equals(orderDetail.getItemId())
                                )
                                .sorted(Comparator.comparing(Ticket::getId).reversed())
                                .map(ticket -> ticket.getShowtime().getId())
                                .findFirst()
                                .orElse(null);

                        if (detectedShowtimeId != null) break;
                    }
                }
            }

            if (order.getOrderDetails() != null) {
                for (OrderDetail detail : order.getOrderDetails()) {
                    String itemType = String.valueOf(detail.getItemType());

                    if ("TICKET".equalsIgnoreCase(itemType)) {
                        if (detectedShowtimeId != null) {
                            List<Ticket> tickets = ticketRepository.findBySeatIdAndShowtimeId(
                                    detail.getItemId(),
                                    detectedShowtimeId
                            );

                            if (!tickets.isEmpty()) {
                                Ticket ticket = tickets.get(0);
                                Showtime showtime = ticket.getShowtime();

                                if (showtime != null) {
                                    movieTitle = showtime.getMovie() != null
                                            ? textOr(showtime.getMovie().getTitle(), movieTitle)
                                            : movieTitle;

                                    roomName = showtime.getRoom() != null
                                            ? textOr(showtime.getRoom().getName(), roomName)
                                            : roomName;

                                    cinemaName = showtime.getCinemaItem() != null
                                            ? textOr(showtime.getCinemaItem().getName(), cinemaName)
                                            : cinemaName;

                                    showDate = formatShowDate(showtime, dateFormatter);
                                    showTime = formatShowTime(showtime, timeFormatter);
                                }

                                commonBookingCode = ticket.getBookingCode() != null
                                        ? ticket.getBookingCode()
                                        : commonBookingCode;

                                String seatName = ticket.getSeatName() != null
                                        ? ticket.getSeatName()
                                        : ticket.getSeatRow() + ticket.getSeatNumber();

                                seatNames.add(seatName);
                            }
                        }
                    } else if ("COMBO".equalsIgnoreCase(itemType)) {
                        String comboName = comboRepository.findById(detail.getItemId())
                                .map(Combo::getName)
                                .orElse("Combo Bắp Nước");

                        comboDetails.add(escape(comboName) + " <span style='color:#f4d419;'>x" + escape(detail.getQuantity()) + "</span>");
                    }
                }
            }

            Collections.sort(seatNames);

            String seatsString = seatNames.isEmpty()
                    ? "N/A"
                    : escape(String.join(", ", seatNames));

            String combosString = comboDetails.isEmpty()
                    ? "<span style='color:#64748b;'>Không đăng ký bắp nước</span>"
                    : String.join("<br/>", comboDetails);

            String qrCodeUrl =
                    "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" +
                            URLEncoder.encode(commonBookingCode, StandardCharsets.UTF_8);

            StringBuilder content = new StringBuilder();

            content.append("<div style='background:#080b14; padding:42px 14px; font-family:Arial, Helvetica, sans-serif; color:#f8fafc;'>");
            content.append("<div style='max-width:560px; margin:0 auto;'>");

            content.append("<div style='background:#0b1020; border:1px solid rgba(255,255,255,0.10); border-radius:28px; overflow:hidden; box-shadow:0 28px 80px rgba(0,0,0,0.55);'>");

            content.append("<div style='height:4px; background:linear-gradient(90deg, transparent, #f4d419, transparent);'></div>");

            content.append("<div style='padding:34px 28px 28px; text-align:center; background:#0b1020;'>");
            content.append("<div style='display:inline-block; padding:6px 14px; border-radius:999px; background:#111827; border:1px solid rgba(244,212,25,0.28); color:#f4d419; font-size:10px; font-weight:900; letter-spacing:2.5px; text-transform:uppercase; margin-bottom:18px;'>");
            content.append("KN Cinema Ticket");
            content.append("</div>");

            content.append("<h1 style='margin:0; color:#ffffff; font-size:30px; line-height:1; font-weight:900; letter-spacing:-1px; text-transform:uppercase;'>");
            content.append("Vé điện tử");
            content.append("</h1>");

            content.append("<p style='margin:12px 0 0; color:#94a3b8; font-size:12px; line-height:1.6; font-weight:700; text-transform:uppercase; letter-spacing:1.2px;'>");
            content.append("Giao dịch đã được xác nhận thành công");
            content.append("</p>");

            content.append("<div style='display:inline-block; margin-top:18px; background:#111827; border:1px solid rgba(255,255,255,0.10); padding:7px 14px; border-radius:12px; color:#e2e8f0; font-size:11px; font-weight:900; letter-spacing:1px;'>");
            content.append("Mã đơn #").append(escape(order.getId()));
            content.append("</div>");
            content.append("</div>");

            content.append("<div style='padding:28px; background:#0d1222; border-top:1px solid rgba(255,255,255,0.08);'>");
            content.append("<div style='font-size:9px; font-weight:900; color:#f4d419; text-transform:uppercase; letter-spacing:2px; margin-bottom:8px;'>");
            content.append("Tác phẩm điện ảnh");
            content.append("</div>");

            content.append("<h2 style='margin:0 0 22px; color:#ffffff; font-size:24px; line-height:1.18; font-weight:900; text-transform:uppercase; letter-spacing:-0.4px;'>");
            content.append(escape(movieTitle));
            content.append("</h2>");

            content.append("<table style='width:100%; border-collapse:separate; border-spacing:0 10px;'>");

            content.append("<tr>");
            content.append("<td style='width:50%; padding:14px; background:#111827; border:1px solid rgba(255,255,255,0.08); border-radius:14px;'>");
            content.append("<span style='display:block; color:#64748b; font-size:9px; font-weight:900; text-transform:uppercase; letter-spacing:1.5px; margin-bottom:6px;'>Ngày chiếu</span>");
            content.append("<strong style='color:#f8fafc; font-size:14px;'>").append(escape(showDate)).append("</strong>");
            content.append("</td>");

            content.append("<td style='width:50%; padding:14px; background:#111827; border:1px solid rgba(255,255,255,0.08); border-radius:14px;'>");
            content.append("<span style='display:block; color:#64748b; font-size:9px; font-weight:900; text-transform:uppercase; letter-spacing:1.5px; margin-bottom:6px;'>Suất chiếu</span>");
            content.append("<strong style='color:#67e8f9; font-size:14px;'>").append(escape(showTime)).append("</strong>");
            content.append("</td>");
            content.append("</tr>");

            content.append("<tr>");
            content.append("<td style='width:50%; padding:14px; background:#111827; border:1px solid rgba(255,255,255,0.08); border-radius:14px;'>");
            content.append("<span style='display:block; color:#64748b; font-size:9px; font-weight:900; text-transform:uppercase; letter-spacing:1.5px; margin-bottom:6px;'>Rạp / Phòng</span>");
            content.append("<strong style='color:#f8fafc; font-size:13px; line-height:1.35;'>").append(escape(cinemaName)).append("<br/><span style='color:#f4d419;'>").append(escape(roomName)).append("</span></strong>");
            content.append("</td>");

            content.append("<td style='width:50%; padding:14px; background:#111827; border:1px solid rgba(255,255,255,0.08); border-radius:14px;'>");
            content.append("<span style='display:block; color:#64748b; font-size:9px; font-weight:900; text-transform:uppercase; letter-spacing:1.5px; margin-bottom:6px;'>Vị trí ghế</span>");
            content.append("<strong style='color:#ffffff; font-size:18px; font-weight:900;'>").append(seatsString).append("</strong>");
            content.append("</td>");
            content.append("</tr>");

            content.append("</table>");
            content.append("</div>");

            content.append("<div style='padding:34px 28px; background:#0b1020; text-align:center; border-top:1px dashed rgba(255,255,255,0.14); border-bottom:1px dashed rgba(255,255,255,0.14);'>");
            content.append("<div style='font-size:10px; font-weight:900; color:#94a3b8; letter-spacing:2px; margin-bottom:16px; text-transform:uppercase;'>");
            content.append("Mã QR soát vé tại quầy");
            content.append("</div>");

            content.append("<div style='display:inline-block; padding:14px; background:#ffffff; border-radius:22px; box-shadow:0 18px 42px rgba(0,0,0,0.45);'>");
            content.append("<img src='").append(escape(qrCodeUrl)).append("' style='display:block; border:0;' width='160' height='160' alt='QR Code'/>");
            content.append("</div>");

            content.append("<div style='margin:18px auto 0; max-width:260px; background:#111827; border:1px solid rgba(255,255,255,0.10); color:#ffffff; padding:10px 14px; border-radius:14px; font-size:18px; font-weight:900; letter-spacing:4px; font-family:monospace;'>");
            content.append(escape(commonBookingCode));
            content.append("</div>");

            content.append("<p style='margin:12px auto 0; max-width:360px; color:#64748b; font-size:11px; line-height:1.6; font-weight:700;'>");
            content.append("Đưa mã này cho nhân viên quầy soát vé để quét QR và nhận vé cứng / combo đi kèm.");
            content.append("</p>");
            content.append("</div>");

            content.append("<div style='padding:28px; background:#0d1222;'>");
            content.append("<table style='width:100%; border-collapse:collapse; font-size:13px; color:#94a3b8;'>");

            content.append("<tr>");
            content.append("<td style='padding:0 0 12px;'>Khách hàng</td>");
            content.append("<td style='padding:0 0 12px; text-align:right; color:#ffffff; font-weight:900;'>").append(escape(customerName)).append("</td>");
            content.append("</tr>");

            content.append("<tr>");
            content.append("<td style='padding:0 0 12px; vertical-align:top;'>Dịch vụ đi kèm</td>");
            content.append("<td style='padding:0 0 12px; text-align:right; color:#ffffff; font-weight:800; line-height:1.5;'>").append(combosString).append("</td>");
            content.append("</tr>");

            content.append("<tr>");
            content.append("<td colspan='2' style='height:1px; background:rgba(255,255,255,0.10);'></td>");
            content.append("</tr>");

            content.append("<tr>");
            content.append("<td style='padding-top:18px; color:#ffffff; font-size:14px; font-weight:900;'>Tổng thanh toán</td>");
            content.append("<td style='padding-top:18px; text-align:right; color:#f4d419; font-size:24px; font-weight:900;'>").append(escape(totalAmountFormatted)).append("</td>");
            content.append("</tr>");

            content.append("</table>");
            content.append("</div>");

            content.append("<div style='background:#080b14; padding:20px; text-align:center; border-top:1px solid rgba(255,255,255,0.08);'>");
            content.append("<p style='margin:0 0 5px; color:#64748b; font-size:10px; font-weight:900; letter-spacing:1.2px; text-transform:uppercase;'>");
            content.append("Hệ thống điện tử phát hành vé tự động KN Cinema");
            content.append("</p>");
            content.append("<p style='margin:0; color:#475569; font-size:10px; font-weight:700;'>© 2026 KN Cinema. All rights reserved.</p>");
            content.append("</div>");

            content.append("</div>");
            content.append("</div>");
            content.append("</div>");

            helper.setText(content.toString(), true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail đặt vé: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendShowtimeCancellationEmail(User user, Showtime showtime, int points, boolean isSystemAuto) {
        try {
            if (user == null || user.getEmail() == null || showtime == null) {
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("kienphatanh@gmail.com", "KN Cinema Support");
            helper.setTo(user.getEmail());
            helper.setSubject("KN CINEMA - THÔNG BÁO HỦY SUẤT CHIẾU & ĐỀN BÙ ĐIỂM THƯỞNG");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            String customerName = getCustomerName(user);
            String movieTitle = showtime.getMovie() != null
                    ? textOr(showtime.getMovie().getTitle(), "Phim Điện Ảnh")
                    : "Phim Điện Ảnh";

            String showDate = formatShowDate(showtime, dateFormatter);
            String showTime = formatShowTime(showtime, timeFormatter);

            String roomName = showtime.getRoom() != null
                    ? textOr(showtime.getRoom().getName(), "N/A")
                    : "N/A";

            String cinemaName = showtime.getCinemaItem() != null
                    ? textOr(showtime.getCinemaItem().getName(), "Hệ thống KN Cinema")
                    : "Hệ thống KN Cinema";

            String reasonMessage = isSystemAuto
                    ? "KN Cinema vô cùng xin lỗi quý khách. Do suất chiếu không đạt đủ số lượng vé tối thiểu để vận hành, hệ thống buộc phải hủy suất chiếu này."
                    : "KN Cinema vô cùng xin lỗi quý khách. Do sự cố kỹ thuật đột xuất tại rạp, suất chiếu của quý khách đã buộc phải hủy bỏ.";

            StringBuilder content = new StringBuilder();

            content.append("<div style='background:#080b14; padding:42px 14px; font-family:Arial, Helvetica, sans-serif; color:#f8fafc;'>");
            content.append("<div style='max-width:560px; margin:0 auto;'>");

            content.append("<div style='background:#0b1020; border:1px solid rgba(255,255,255,0.10); border-radius:28px; overflow:hidden; box-shadow:0 28px 80px rgba(0,0,0,0.55);'>");

            content.append("<div style='height:4px; background:linear-gradient(90deg, transparent, #fb7185, transparent);'></div>");

            content.append("<div style='padding:34px 28px 28px; text-align:center; background:#0b1020;'>");
            content.append("<div style='display:inline-block; padding:6px 14px; border-radius:999px; background:#111827; border:1px solid rgba(251,113,133,0.35); color:#fb7185; font-size:10px; font-weight:900; letter-spacing:2.5px; text-transform:uppercase; margin-bottom:18px;'>");
            content.append("Thông báo quan trọng");
            content.append("</div>");

            content.append("<h1 style='margin:0; color:#ffffff; font-size:28px; line-height:1; font-weight:900; letter-spacing:-1px; text-transform:uppercase;'>");
            content.append("Hủy suất chiếu");
            content.append("</h1>");

            content.append("<p style='margin:12px auto 0; max-width:380px; color:#94a3b8; font-size:12px; line-height:1.6; font-weight:700;'>");
            content.append("KN Cinema rất tiếc phải thông báo suất chiếu của quý khách đã bị hủy.");
            content.append("</p>");
            content.append("</div>");

            content.append("<div style='padding:28px; background:#0d1222;'>");
            content.append("<p style='margin:0 0 14px; color:#dbeafe; font-size:14px; line-height:1.7;'>");
            content.append("Kính gửi quý khách <strong style='color:#ffffff;'>").append(escape(customerName)).append("</strong>,");
            content.append("</p>");

            content.append("<p style='margin:0; color:#cbd5e1; font-size:13px; line-height:1.7;'>");
            content.append(escape(reasonMessage));
            content.append("</p>");

            content.append("<div style='margin:26px 0; background:#111827; border:1px solid rgba(244,212,25,0.28); border-radius:18px; padding:24px 18px; text-align:center;'>");
            content.append("<span style='font-size:10px; font-weight:900; color:#f4d419; text-transform:uppercase; letter-spacing:2px; display:block; margin-bottom:10px;'>");
            content.append("Đã hoàn trả điểm thưởng tự động");
            content.append("</span>");

            content.append("<h2 style='margin:0; font-size:40px; line-height:1; font-weight:900; color:#f4d419;'>");
            content.append("+").append(escape(points));
            content.append("<span style='font-size:16px; color:#ffffff; margin-left:6px;'>Điểm</span>");
            content.append("</h2>");

            content.append("<p style='margin:13px auto 0; max-width:360px; font-size:12px; color:#94a3b8; line-height:1.6; font-weight:700;'>");
            content.append("Quý khách có thể dùng điểm này để đổi mã giảm giá hoặc sử dụng cho lần đặt vé tiếp theo.");
            content.append("</p>");
            content.append("</div>");

            content.append("<div style='font-size:9px; font-weight:900; color:#fb7185; text-transform:uppercase; letter-spacing:2px; margin-bottom:8px;'>");
            content.append("Thông tin suất chiếu đã hủy");
            content.append("</div>");

            content.append("<h2 style='margin:0 0 20px; color:#ffffff; font-size:22px; line-height:1.2; font-weight:900; text-transform:uppercase; letter-spacing:-0.3px; text-decoration:line-through; text-decoration-color:#fb7185;'>");
            content.append(escape(movieTitle));
            content.append("</h2>");

            content.append("<table style='width:100%; border-collapse:separate; border-spacing:0 10px;'>");

            content.append("<tr>");
            content.append("<td style='width:50%; padding:14px; background:#111827; border:1px solid rgba(255,255,255,0.08); border-radius:14px;'>");
            content.append("<span style='display:block; color:#64748b; font-size:9px; font-weight:900; text-transform:uppercase; letter-spacing:1.5px; margin-bottom:6px;'>Ngày chiếu</span>");
            content.append("<strong style='color:#f8fafc; font-size:14px;'>").append(escape(showDate)).append("</strong>");
            content.append("</td>");

            content.append("<td style='width:50%; padding:14px; background:#111827; border:1px solid rgba(255,255,255,0.08); border-radius:14px;'>");
            content.append("<span style='display:block; color:#64748b; font-size:9px; font-weight:900; text-transform:uppercase; letter-spacing:1.5px; margin-bottom:6px;'>Suất chiếu</span>");
            content.append("<strong style='color:#67e8f9; font-size:14px;'>").append(escape(showTime)).append("</strong>");
            content.append("</td>");
            content.append("</tr>");

            content.append("<tr>");
            content.append("<td style='width:50%; padding:14px; background:#111827; border:1px solid rgba(255,255,255,0.08); border-radius:14px;'>");
            content.append("<span style='display:block; color:#64748b; font-size:9px; font-weight:900; text-transform:uppercase; letter-spacing:1.5px; margin-bottom:6px;'>Rạp chiếu</span>");
            content.append("<strong style='color:#f8fafc; font-size:13px; line-height:1.35;'>").append(escape(cinemaName)).append("</strong>");
            content.append("</td>");

            content.append("<td style='width:50%; padding:14px; background:#111827; border:1px solid rgba(255,255,255,0.08); border-radius:14px;'>");
            content.append("<span style='display:block; color:#64748b; font-size:9px; font-weight:900; text-transform:uppercase; letter-spacing:1.5px; margin-bottom:6px;'>Phòng chiếu</span>");
            content.append("<strong style='color:#f4d419; font-size:14px; font-weight:900;'>").append(escape(roomName)).append("</strong>");
            content.append("</td>");
            content.append("</tr>");

            content.append("</table>");
            content.append("</div>");

            content.append("<div style='background:#080b14; padding:22px 24px; text-align:center; border-top:1px solid rgba(255,255,255,0.08);'>");
            content.append("<p style='margin:0 0 8px; color:#cbd5e1; font-size:12px; line-height:1.6; font-weight:700;'>");
            content.append("Một lần nữa, KN Cinema thành thật xin lỗi vì sự bất tiện này.");
            content.append("</p>");

            content.append("<p style='margin:0; color:#64748b; font-size:10px; font-weight:900; letter-spacing:1.2px; text-transform:uppercase;'>");
            content.append("Hệ thống chăm sóc khách hàng KN Cinema");
            content.append("</p>");
            content.append("</div>");

            content.append("</div>");
            content.append("</div>");
            content.append("</div>");

            helper.setText(content.toString(), true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail thông báo hủy: " + e.getMessage());
        }
    }
}