package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.TicketRequest;
import com.example.cinema.entity.Ticket;
import com.example.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Ticket>> createTicket(@RequestBody TicketRequest request) {
        return ResponseEntity.ok(ApiResponse.<Ticket>builder()
                .status(201)
                .message("Đặt vé thành công")
                .data(ticketService.createTicket(request))
                .build());
    }

    @GetMapping("/my-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Ticket>>> getMyHistory() {
        return ResponseEntity.ok(ApiResponse.<List<Ticket>>builder()
                .status(200)
                .message("Lấy lịch sử vé thành công")
                .data(ticketService.getMyTickets())
                .build());
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("isAuthenticated()") // Cần đăng nhập để check mã vé
    public ResponseEntity<ApiResponse<Ticket>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.<Ticket>builder()
                .status(200)
                .message("Thành công")
                .data(ticketService.getByBookingCode(code))
                .build());
    }

    // --- API DÀNH CHO ADMIN ---

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Ticket>>> getAllTickets() {
        return ResponseEntity.ok(ApiResponse.<List<Ticket>>builder()
                .status(200)
                .message("Lấy danh sách vé theo quyền hạn quản lý thành công")
                .data(ticketService.getAllTickets())
                .build());
    }

    @GetMapping("/showtime/{showtimeId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Ticket>>> getTicketsByShowtime(@PathVariable Long showtimeId) {
        return ResponseEntity.ok(ApiResponse.<List<Ticket>>builder()
                .status(200)
                .message("Lấy danh sách vé của suất chiếu thành công")
                .data(ticketService.getTicketsByShowtime(showtimeId))
                .build());
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelTicket(@PathVariable Long id) {
        ticketService.cancelTicket(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(200)
                .message("Đã hủy vé thành công")
                .build());
    }
}