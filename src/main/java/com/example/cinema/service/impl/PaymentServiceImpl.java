package com.example.cinema.service.impl;

import com.example.cinema.dto.PaymentResponse;
import com.example.cinema.entity.Order;
import com.example.cinema.entity.Payment;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.OrderRepository;
import com.example.cinema.repository.PaymentRepository;
import com.example.cinema.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public PaymentResponse createPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        // Kiểm tra xem đơn hàng đã có thanh toán chưa
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(new Payment());
        
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus("PENDING");
        
        // Giả lập tạo nội dung QR (Ví dụ: VietQR format)
        String qrData = "20081015|BANK_ID|ACCOUNT_NO|" + order.getTotalAmount() + "|ORDER" + orderId;
        payment.setQrContent(qrData);

        payment = paymentRepository.save(payment);
        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));

        payment.setStatus("COMPLETED");
        payment.setPaidAt(LocalDateTime.now());
        
        // Cập nhật luôn trạng thái của Order sang PAID
        Order order = payment.getOrder();
        order.setStatus("PAID");
        orderRepository.save(order);

        return mapToResponse(paymentRepository.save(payment));
    }

    @Override
    public Payment getPaymentByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Thanh toán không tồn tại"));
    }

    private PaymentResponse mapToResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .amount(p.getAmount())
                .status(p.getStatus())
                .qrContent(p.getQrContent())
                .orderId(p.getOrder().getId())
                .createdAt(p.getCreatedAt())
                .build();
    }
}