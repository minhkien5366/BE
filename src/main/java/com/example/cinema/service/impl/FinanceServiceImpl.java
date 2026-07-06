package com.example.cinema.service.impl;

import com.example.cinema.repository.OrderRepository;
import com.example.cinema.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class FinanceServiceImpl implements FinanceService {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Map<String, Object> getMonthlyFinance(String month, Double taxRatePercent) {
        // Khởi tạo khoảng thời gian trong tháng
        LocalDate startDate = LocalDate.parse(month + "-01");
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        // Tính tổng doanh thu thô (Gross Revenue)
        Double gross = orderRepository.sumRevenue(start, end);
        gross = gross == null ? 0 : gross;

        // Xử lý giá trị mặc định nếu người dùng không nhập hoặc nhập lỗi rate thuế
        double currentTaxRate = (taxRatePercent == null || taxRatePercent < 0) ? 0.0 : taxRatePercent / 100.0;

        // Tính toán động dựa trên mức thuế người dùng truyền vào
        double tax = gross * currentTaxRate;
        double profit = gross - tax;

        // Đếm số lượng đơn hàng đã thanh toán thành công
        Long orderCount = orderRepository
                .countByCreatedAtBetweenAndStatus(start, end, "PAID");

        // Đóng gói dữ liệu trả về
        Map<String, Object> res = new HashMap<>();
        res.put("grossRevenue", gross);
        res.put("taxRatePercent", currentTaxRate * 100); // Trả lại phần trăm hiển thị ở giao diện
        res.put("tax", tax);
        res.put("profit", profit);
        res.put("orderCount", orderCount);

        return res;
    }
}