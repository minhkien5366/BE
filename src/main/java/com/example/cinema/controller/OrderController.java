package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.OrderRequest;
import com.example.cinema.dto.OrderResponse;
import com.example.cinema.service.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.cinema.exception.ResourceNotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 🔥 Cho phép ứng dụng qr-app (port 3001) gọi sang thoải mái không bị chặn CORS
public class OrderController {

    private final OrderService orderService;

    // Đường dẫn trang Frontend mà ông muốn chuyển về sau khi thanh toán
    // @Value("${frontend.url:https://akcinema.vercel.app}")
    @Value("${frontend.url:http://localhost:3000}")
    
    private String frontendUrl;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(201).body(ApiResponse.<OrderResponse>builder()
                .status(201)
                .message("Đặt đơn hàng thành công!")
                .data(response)
                .build());
    }

    /**
     * Endpoint nhận phản hồi tự động từ hệ thống VNPAY
     */
    @GetMapping("/vnpay-callback")
    public void vnpayCallback(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef"); // Đây chính là Order ID
        String amount = params.get("vnp_Amount"); // Lấy thêm số tiền để Frontend hiển thị

        Long orderId = Long.parseLong(txnRef);

        // Cập nhật trạng thái vào Database
        if ("00".equals(responseCode)) {
            orderService.updateOrderStatus(orderId, "PAID");
        } else {
            orderService.updateOrderStatus(orderId, "CANCELLED");
        }

        // Chuyển hướng trình duyệt về trang kết quả Frontend khách hàng
        String redirectUrl = frontendUrl + "/booking/payment/result"
                + "?vnp_ResponseCode=" + responseCode
                + "&vnp_TxnRef=" + txnRef
                + "&vnp_Amount=" + (amount != null ? amount : "0");

        // Gửi lệnh Redirect (Mã 302)
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/my-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyHistory() {
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .status(200)
                .message("Lấy lịch sử mua vé thành công")
                .data(orderService.getMyOrders())
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .status(200)
                .message("Lấy danh sách đơn hàng thành công")
                .data(orderService.getAllOrders())
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .status(200)
                .message("Lấy chi tiết đơn hàng thành công")
                .data(orderService.getOrderById(id))
                .build());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        OrderResponse updatedOrder = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .status(200)
                .message("Cập nhật trạng thái thành công!")
                .data(updatedOrder)
                .build());
    }

    // ================= 🔥 ENDPOINT 1: XỬ LÝ KIỂM TRA MÃ QR TOÀN DIỆN (CHẤP CẢ VỎ CỦA FE CŨ LẪN MỚI) =================
    @GetMapping("/scan")
    @PreAuthorize("hasRole('ADMIN')") 
    public ResponseEntity<ApiResponse<OrderResponse>> scanQrCode(
            @RequestParam(value = "bookingCode", required = false) String bookingCode,
            @RequestParam(value = "qrContent", required = false) String qrContent) {
        
        // 🎯 CƠ CHẾ PHÒNG VỆ: Frontend truyền key nào lên (bookingCode hay qrContent) hệ thống đều tự bắt được
        String finalCode = (bookingCode != null && !bookingCode.trim().isEmpty()) ? bookingCode : qrContent;

        if (finalCode == null || finalCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.<OrderResponse>builder()
                    .status(400)
                    .message("Mã đặt vé trống hoặc không hợp lệ, không thể thực hiện tra cứu!")
                    .build());
        }

        try {
            // Loại bỏ tiền tố chuỗi cứng cũ nếu Frontend lỡ đính kèm vào mã QR
            String cleanCode = finalCode.replace("AK-CINEMA-ORDER-", "").trim();

            // 🔥 THỰC THI CHUẨN: Gọi hàm quét theo chuỗi ký tự String bookingCode đã làm sạch
            OrderResponse orderResponse = orderService.scanOrderTicket(cleanCode);

            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .status(200)
                    .message("Xác thực mã đặt vé thành công!")
                    .data(orderResponse)
                    .build());

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.<OrderResponse>builder()
                    .status(404)
                    .message(e.getMessage())
                    .build());
        } catch (RuntimeException e) {
            // Trả trực tiếp các câu Runtime thông minh: "CẢNH BÁO GIAN LẬN", "Sai chi nhánh", "Vé hết hạn" về màn hình POS
            return ResponseEntity.badRequest().body(ApiResponse.<OrderResponse>builder()
                    .status(400)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<OrderResponse>builder()
                    .status(500)
                    .message("Lỗi hệ thống khi truy xuất mã vé: " + e.getMessage())
                    .build());
        }
    }

    // ================= 🔥 ENDPOINT 2: XÁC NHẬN SỬ DỤNG VÉ (ĐỔI SANG TRẠNG THÁI USED) =================
    @PutMapping("/{id}/confirm-checkin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmCheckIn(@PathVariable Long id) {
        try {
            // Chuyển trạng thái Order + Tickets sang trạng thái USED
            OrderResponse updatedOrder = orderService.confirmCheckIn(id);
            
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .status(200)
                    .message("Đã hoàn tất thủ tục bàn giao vé cứng & bắp nước thành công!")
                    .data(updatedOrder)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<OrderResponse>builder()
                    .status(400)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<OrderResponse>builder()
                    .status(500)
                    .message("Lỗi hệ thống khi xác nhận check-in đơn hàng: " + e.getMessage())
                    .build());
        }
    }
}