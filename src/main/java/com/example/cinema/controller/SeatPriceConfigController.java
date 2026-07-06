package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.SeatPriceConfigRequest;
import com.example.cinema.entity.SeatPriceConfig;
import com.example.cinema.service.SeatPriceConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seat-price-configs")
@RequiredArgsConstructor
public class SeatPriceConfigController {

    private final SeatPriceConfigService service;

    /**
     * Lấy danh sách cấu hình giá
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SeatPriceConfig>>> getAll() {

        return ResponseEntity.ok(
                ApiResponse.<List<SeatPriceConfig>>builder()
                        .status(200)
                        .message("Lấy danh sách cấu hình giá thành công")
                        .data(service.getAllConfigs())
                        .build()
        );
    }

    /**
     * Thêm cấu hình giá
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SeatPriceConfig>> create(
            @Valid @RequestBody SeatPriceConfigRequest request
    ) {

        return ResponseEntity.status(201).body(
                ApiResponse.<SeatPriceConfig>builder()
                        .status(201)
                        .message("Thêm cấu hình giá thành công")
                        .data(service.createConfig(request))
                        .build()
        );
    }

    /**
     * Cập nhật cấu hình giá
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SeatPriceConfig>> update(
            @PathVariable Long id,
            @Valid @RequestBody SeatPriceConfigRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.<SeatPriceConfig>builder()
                        .status(200)
                        .message("Cập nhật thành công")
                        .data(service.updateConfig(id, request))
                        .build()
        );
    }

    /**
     * Xoá cấu hình giá
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id
    ) {

        service.deleteConfig(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .status(200)
                        .message("Xoá cấu hình giá thành công")
                        .build()
        );
    }
}