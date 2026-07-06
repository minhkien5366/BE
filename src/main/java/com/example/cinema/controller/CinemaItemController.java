package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.CinemaItemRequest;
import com.example.cinema.entity.CinemaItem;
import com.example.cinema.service.CinemaItemService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cinema-items")
@RequiredArgsConstructor
public class CinemaItemController {

    private final CinemaItemService itemService;

    // ================= GET ALL =================
    // Hỗ trợ:
    // /api/v1/cinema-items
    // /api/v1/cinema-items?city=HCM
    // /api/v1/cinema-items?cinemaId=1
    @GetMapping
    public ResponseEntity<ApiResponse<List<CinemaItem>>> getAll(

            @RequestParam(required = false)
            String city,

            @RequestParam(required = false)
            Long cinemaId
    ) {

        List<CinemaItem> data;

        // ================= FILTER CITY =================
        if (city != null && !city.trim().isEmpty()) {

            data = itemService.getByCity(
                    city.trim()
            );

        }

        // ================= FILTER CINEMA =================
        else if (cinemaId != null) {

            data = itemService.getByCinema(
                    cinemaId
            );

        }

        // ================= GET ALL =================
        else {

            data = itemService.getAllItems();
        }

        return ResponseEntity.ok(

                ApiResponse.<List<CinemaItem>>builder()
                        .status(200)
                        .message(
                                "Lấy danh sách chi nhánh thành công"
                        )
                        .data(data)
                        .build()
        );
    }

    // ================= GET DETAIL =================
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaItem>> getById(
            @PathVariable Long id
    ) {

        CinemaItem item =
                itemService.getItemById(id);

        return ResponseEntity.ok(

                ApiResponse.<CinemaItem>builder()
                        .status(200)
                        .message(
                                "Lấy thông tin chi nhánh thành công"
                        )
                        .data(item)
                        .build()
        );
    }

    // ================= CREATE =================
    @PostMapping
    @PreAuthorize(
            "hasRole('SUPER_ADMIN')"
    )
    public ResponseEntity<ApiResponse<CinemaItem>> create(

            @Valid
            @RequestBody
            CinemaItemRequest request
    ) {

        CinemaItem newItem =
                itemService.createItem(request);

        return ResponseEntity.status(201).body(

                ApiResponse.<CinemaItem>builder()
                        .status(201)
                        .message(
                                "Đã tạo chi nhánh mới thành công"
                        )
                        .data(newItem)
                        .build()
        );
    }

    // ================= UPDATE =================
    @PutMapping("/{id}")
    @PreAuthorize(
            "hasRole('SUPER_ADMIN')"
    )
    public ResponseEntity<ApiResponse<CinemaItem>> update(

            @PathVariable Long id,

            @Valid
            @RequestBody
            CinemaItemRequest request
    ) {

        CinemaItem updatedItem =
                itemService.updateItem(
                        id,
                        request
                );

        return ResponseEntity.ok(

                ApiResponse.<CinemaItem>builder()
                        .status(200)
                        .message(
                                "Đã cập nhật chi nhánh thành công"
                        )
                        .data(updatedItem)
                        .build()
        );
    }

    // ================= DELETE =================
    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasRole('SUPER_ADMIN')"
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id
    ) {

        itemService.deleteItem(id);

        return ResponseEntity.ok(

                ApiResponse.<Void>builder()
                        .status(200)
                        .message(
                                "Đã xóa chi nhánh thành công"
                        )
                        .build()
        );
    }
}