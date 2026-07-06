package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.SeatRequest;
import com.example.cinema.entity.Seat;
import com.example.cinema.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    // =================🔥 API REAL-TIME: KHÓA VÀ NHẢ GHẾ TỨC THÌ 🔥=================
    @PostMapping("/hold")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> holdSeat(
            @RequestParam Long showtimeId,
            @RequestParam Long seatId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        seatService.holdSeat(showtimeId, seatId, email);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(200)
                .message("Đã giữ ghế thành công")
                .build());
    }

    @PostMapping("/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> releaseSeat(
            @RequestParam Long showtimeId,
            @RequestParam Long seatId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        seatService.releaseSeat(showtimeId, seatId, email);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(200)
                .message("Đã nhả ghế thành công")
                .build());
    }
    // =========================================================================

    @GetMapping("/showtime/{showtimeId}")
    public ResponseEntity<ApiResponse<List<Seat>>> getSeatsByShowtime(@PathVariable Long showtimeId) {
        return ResponseEntity.ok(ApiResponse.<List<Seat>>builder()
                .status(200)
                .message("Lấy trạng thái ghế theo suất chiếu thành công")
                .data(seatService.getSeatsByShowtime(showtimeId))
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Seat>>> getAllSeats() {
        return ResponseEntity.ok(ApiResponse.<List<Seat>>builder()
                .status(200)
                .message("Lấy danh sách ghế theo chi nhánh quản lý thành công")
                .data(seatService.getAllSeats())
                .build());
    }

    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Seat>>> getSeatsByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(ApiResponse.<List<Seat>>builder()
                .status(200)
                .message("Lấy danh sách ghế của phòng thành công")
                .data(seatService.getSeatsByRoom(roomId))
                .build());
    }

    @GetMapping("/{id}/check-tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkSeatEligibility(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<Map<String, Boolean>>builder()
                .status(200)
                .message("Kiểm tra điều kiện xóa ghế thành công")
                .data(seatService.checkSeatEligibility(id))
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Seat>> createSeat(@RequestBody SeatRequest request) {
        return ResponseEntity.ok(ApiResponse.<Seat>builder()
                .status(201)
                .message("Tạo ghế thành công")
                .data(seatService.createSeat(request))
                .build());
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Seat>>> generateSeats(
            @RequestParam Long roomId,
            @RequestParam int rows,
            @RequestParam int seatsPerRow) {
        return ResponseEntity.ok(ApiResponse.<List<Seat>>builder()
                .status(201)
                .message("Sinh danh sách ghế tự động cho phòng thành công")
                .data(seatService.generateSeatsForRoom(roomId, rows, seatsPerRow))
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Seat>> updateSeat(@PathVariable Long id, @RequestBody SeatRequest request) {
        return ResponseEntity.ok(ApiResponse.<Seat>builder()
                .status(200)
                .message("Cập nhật ghế thành công")
                .data(seatService.updateSeat(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteSeat(@PathVariable Long id) {
        seatService.deleteSeat(id);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(200)
                .message("Xóa ghế thành công")
                .build());
    }

    @DeleteMapping("/room/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteSeatsByRoom(@PathVariable Long roomId) {
        seatService.deleteSeatsByRoom(roomId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(200)
                .message("Đã xóa toàn bộ ghế của phòng " + roomId)
                .build());
    }
}