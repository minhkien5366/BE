package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.PaymentResponse;
import com.example.cinema.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Tạo mã thanh toán cho đơn hàng vừa đặt
    @PostMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .status(200)
                .message("Tạo mã thanh toán thành công")
                .data(paymentService.createPayment(orderId))
                .build());
    }

    // Admin xác nhận đã nhận được tiền (Test Postman)
    @PutMapping("/confirm/{orderId}")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirm(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .status(200)
                .message("Xác nhận thanh toán thành công")
                .data(paymentService.confirmPayment(orderId))
                .build());
    }
}