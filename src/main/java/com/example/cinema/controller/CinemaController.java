package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.CinemaRequest;
import com.example.cinema.entity.Cinema;
import com.example.cinema.service.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cinemas")
@RequiredArgsConstructor
public class CinemaController {
    private final CinemaService cinemaService;

    // 1. Lấy danh sách tất cả cụm rạp (Công khai)
    @GetMapping
    public ResponseEntity<ApiResponse<List<Cinema>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<Cinema>>builder()
                .status(200)
                .message("Lấy danh sách cụm rạp thành công")
                .data(cinemaService.getAllCinemas())
                .build());
    }

    // 2. Lấy chi tiết một cụm rạp theo ID (Đã cập nhật hết lỗi 501)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Cinema>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<Cinema>builder()
                .status(200)
                .message("Lấy thông tin cụm rạp thành công")
                .data(cinemaService.getCinemaById(id))
                .build());
    }

    // 3. Tạo cụm rạp mới (Chỉ Super Admin)
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Cinema>> create(@RequestBody CinemaRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.<Cinema>builder()
                .status(201)
                .message("Đã tạo cụm rạp thành công")
                .data(cinemaService.createCinema(request))
                .build());
    }

    // 4. Cập nhật cụm rạp (Chỉ Super Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Cinema>> update(@PathVariable Long id, @RequestBody CinemaRequest request) {
        return ResponseEntity.ok(ApiResponse.<Cinema>builder()
                .status(200)
                .message("Cập nhật cụm rạp thành công")
                .data(cinemaService.updateCinema(id, request))
                .build());
    }

    // 5. Xóa cụm rạp (Chỉ Super Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        cinemaService.deleteCinema(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(200)
                .message("Đã xóa cụm rạp thành công")
                .build());
    }
}