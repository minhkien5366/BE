package com.example.cinema.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String status;
    private Double totalAmount;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private Long cinemaItemId;
    private String cinemaName;
    private List<OrderDetailResponse> orderDetails;
    private String paymentUrl;

    // 🎯 THÊM DÒNG NÀY: Để đảm bảo QR Code quét ra chính xác mã trong DB
    private String bookingCode;

    // Các trường suất chiếu đã thêm
    private String movieTitle;
    private String date;
    private String time;
    private String roomName;
}