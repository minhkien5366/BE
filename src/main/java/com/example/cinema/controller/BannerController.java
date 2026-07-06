package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.BannerRequest;
import com.example.cinema.entity.Banner;
import com.example.cinema.service.BannerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Banner>>> getActiveBanners() {
        return ResponseEntity.ok(ApiResponse.<List<Banner>>builder()
                .status(200).data(bannerService.getActiveBanners()).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Banner>>> getAllBanners() {
        return ResponseEntity.ok(ApiResponse.<List<Banner>>builder()
                .status(200).data(bannerService.getAllBanners()).build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Banner>> createBanner(
            @Valid @RequestPart("banner") BannerRequest request, 
            @RequestPart(value = "file", required = false) MultipartFile file) {
        Banner banner = bannerService.createBanner(request, file);
        return ResponseEntity.ok(ApiResponse.<Banner>builder()
                .status(201).message("Thêm banner thành công").data(banner).build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Banner>> updateBanner(
            @PathVariable Long id,
            @Valid @RequestPart("banner") BannerRequest request, 
            @RequestPart(value = "file", required = false) MultipartFile file) {
        Banner banner = bannerService.updateBanner(id, request, file);
        return ResponseEntity.ok(ApiResponse.<Banner>builder()
                .status(200).message("Cập nhật banner thành công").data(banner).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(200).message("Xóa banner thành công").build());
    }
}