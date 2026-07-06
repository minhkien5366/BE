package com.example.cinema.service;

import com.example.cinema.dto.OrderRequest;
import com.example.cinema.dto.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    OrderResponse getOrderById(Long id);
    List<OrderResponse> getMyOrders();
    List<OrderResponse> getAllOrders(); // Lọc theo quyền Admin
    OrderResponse updateOrderStatus(Long orderId, String newStatus);
    OrderResponse scanOrderTicket(String bookingCode);
    OrderResponse confirmCheckIn(Long orderId);
    String cancelOrder(Long orderId);
}