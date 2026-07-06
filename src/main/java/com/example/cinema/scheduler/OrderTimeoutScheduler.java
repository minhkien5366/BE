package com.example.cinema.scheduler;

import com.example.cinema.entity.Order;
import com.example.cinema.repository.OrderRepository;
import com.example.cinema.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void checkAndCancelExpiredOrders() {
        log.info("⏰ Bắt đầu quét ngầm hệ thống tìm đơn hàng quá hạn...");

        // Mốc thời gian chặn: Hiện tại trừ đi 2 phút
        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(2);

        // Lấy ra tất cả đơn hàng vẫn đang PENDING mà đã tạo lâu hơn 2 phút trước
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore("PENDING", thresholdTime);

        if (!expiredOrders.isEmpty()) {
            log.info("🔥 Phát hiện {} đơn hàng bị treo quá 2 phút! Tiến hành hủy đơn...", expiredOrders.size());
            
            for (Order order : expiredOrders) {
                try {
                    // Gọi hàm hủy đơn để trả lại ghế và kho
                    orderService.cancelOrder(order.getId());
                    log.info("✅ Đã hủy tự động thành công đơn hàng ID: {}", order.getId());
                } catch (Exception e) {
                    log.error("❌ Lỗi xảy ra khi tự động hủy đơn hàng ID {}: {}", order.getId(), e.getMessage());
                }
            }
        } else {
            log.info("🟢 Không phát hiện đơn hàng nào bị treo quá hạn.");
        }
    }
}