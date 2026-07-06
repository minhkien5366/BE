package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.GenreRequest;
import com.example.cinema.entity.Genre;
import com.example.cinema.service.GenreService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Genre>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<Genre>>builder()
                .status(200)
                .message("Lấy danh sách thể loại thành công")
                .data(genreService.getAllGenres())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Genre>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.<Genre>builder()
                .status(200)
                .message("Thành công")
                .data(genreService.getGenreById(id))
                .build());
    }


    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Genre>> create(
            @Valid @RequestBody GenreRequest request) {
        return ResponseEntity.ok(ApiResponse.<Genre>builder()
                .status(201)
                .message("Thêm thể loại thành công")
                .data(genreService.createGenre(request))
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Genre>> update(
            @PathVariable Integer id,
            @Valid @RequestBody GenreRequest request) {
        return ResponseEntity.ok(ApiResponse.<Genre>builder()
                .status(200)
                .message("Cập nhật thành công")
                .data(genreService.updateGenre(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id) {
            genreService.deleteGenre(id);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .status(200)
                    .message("Xóa thể loại thành công")
                    .build());
        }
    }