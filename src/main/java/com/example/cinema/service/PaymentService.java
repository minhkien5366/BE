package com.example.cinema.service;

import com.example.cinema.dto.PaymentResponse;
import com.example.cinema.entity.Payment;

public interface PaymentService {
    // Tạo yêu cầu thanh toán cho một đơn hàng
    PaymentResponse createPayment(Long orderId);

    // Xác nhận đã thanh toán (Dùng cho Webhook hoặc Admin xác nhận tay)
    PaymentResponse confirmPayment(Long orderId);

    // Kiểm tra trạng thái thanh toán
    Payment getPaymentByOrder(Long orderId);
}