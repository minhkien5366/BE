package com.example.cinema.repository;

import com.example.cinema.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Tìm thông tin thanh toán theo mã đơn hàng
    Optional<Payment> findByOrderId(Long orderId);
}