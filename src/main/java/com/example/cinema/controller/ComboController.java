package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.ComboRequest;
import com.example.cinema.entity.Combo;
import com.example.cinema.service.ComboService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    // ================= USER + ADMIN + SUPER ADMIN =================

    @GetMapping
    public ResponseEntity<ApiResponse<List<Combo>>> getAll() {

        return ResponseEntity.ok(
                ApiResponse.<List<Combo>>builder()
                        .status(200)
                        .message("Lấy danh sách combo thành công")
                        .data(comboService.getAllCombos())
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Combo>> getById(@PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.<Combo>builder()
                        .status(200)
                        .message("Lấy combo thành công")
                        .data(comboService.getComboById(id))
                        .build()
        );
    }

    // ================= SUPER ADMIN ONLY =================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Combo>> create(

            @Valid
            @RequestPart("combo") ComboRequest request,

            @RequestPart(value = "file", required = false)
            MultipartFile file
    ) {

        Combo combo = comboService.createCombo(request, file);

        return ResponseEntity.status(201).body(
                ApiResponse.<Combo>builder()
                        .status(201)
                        .message("Thêm combo thành công")
                        .data(combo)
                        .build()
        );
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Combo>> update(

            @PathVariable Long id,

            @Valid
            @RequestPart("combo") ComboRequest request,

            @RequestPart(value = "file", required = false)
            MultipartFile file
    ) {

        Combo combo = comboService.updateCombo(id, request, file);

        return ResponseEntity.ok(
                ApiResponse.<Combo>builder()
                        .status(200)
                        .message("Cập nhật combo thành công")
                        .data(combo)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {

        comboService.deleteCombo(id);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(200)
                        .message("Xóa combo thành công")
                        .data("DELETE_SUCCESS")
                        .build()
        );
    }
}