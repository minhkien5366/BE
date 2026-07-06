package com.example.cinema.controller;

import com.example.cinema.dto.ComboAdminResponse;
import com.example.cinema.dto.ComboClientResponse; 
import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.ComboAdminRequest;
import com.example.cinema.service.CinemaComboService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cinema-combos")
@RequiredArgsConstructor
public class CinemaComboController {

    private final CinemaComboService cinemaComboService;

    @GetMapping
    public List<ComboAdminResponse> getCombos() {
        return cinemaComboService.getCombosForAdmin();
    }

    @PatchMapping("/{comboId}/toggle")
    public Boolean toggle(@PathVariable Long comboId) {
        return cinemaComboService.toggleCombo(comboId);
    }

@PutMapping("/{comboId}/stock")
public ResponseEntity<?> updateStock(
        @PathVariable Long comboId,
        @Valid @RequestBody ComboAdminRequest request
) {

    cinemaComboService.updateComboStock(comboId, request);

    return ResponseEntity.ok().build();
}

    @GetMapping("/{cinemaItemId}/combos")
    public ApiResponse<List<ComboClientResponse>> getActiveCombos(
            @PathVariable Long cinemaItemId) {

        List<ComboClientResponse> combos =
                cinemaComboService.getActiveCombosForCinema(cinemaItemId);

        return ApiResponse.<List<ComboClientResponse>>builder()
                .status(200)
                .message("Lấy combo đang bán thành công")
                .data(combos)
                .build();
    }
}
