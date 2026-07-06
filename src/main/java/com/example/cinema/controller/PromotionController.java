package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.PromotionRequest;
import com.example.cinema.entity.Promotion;
import com.example.cinema.service.PromotionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/client/{cinemaItemId}")
    public ResponseEntity<ApiResponse<List<Promotion>>> getForClient(@PathVariable Long cinemaItemId) {
        return ResponseEntity.ok(ApiResponse.<List<Promotion>>builder()
                .status(200).data(promotionService.getPromotionsForClient(cinemaItemId)).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Promotion>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<Promotion>builder()
                .status(200).data(promotionService.getPromotionById(id)).build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Promotion>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<Promotion>>builder()
                .status(200).data(promotionService.getAllPromotions()).build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Promotion>> create(
            @Valid @ModelAttribute PromotionRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) { // Dùng @RequestParam cho file
        return ResponseEntity.status(201).body(ApiResponse.<Promotion>builder()
                .status(201).message("Tạo sự kiện thành công")
                .data(promotionService.createPromotion(request, file)).build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Promotion>> update(
            @PathVariable Long id,
            @Valid @ModelAttribute PromotionRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) { // Dùng @RequestParam cho file
        return ResponseEntity.ok(ApiResponse.<Promotion>builder()
                .status(200).message("Cập nhật thành công")
                .data(promotionService.updatePromotion(id, request, file)).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(200).message("Xóa sự kiện thành công").build());
    }
}