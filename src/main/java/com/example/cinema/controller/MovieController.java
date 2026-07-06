package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.MovieDTO;
import com.example.cinema.dto.MovieRequest;
import com.example.cinema.entity.Movie;
import com.example.cinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    // 1. Lấy danh sách phim (Public)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MovieDTO>>> getMovies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<MovieDTO> movies = movieService.getMovies(search, status, page, size);
        return ResponseEntity.ok(
                ApiResponse.<Page<MovieDTO>>builder()
                        .status(200)
                        .message("Lấy danh sách phim thành công")
                        .data(movies)
                        .build()
        );
    }

    // 2. Import phim bằng file Excel
    @PostMapping("/import")
    public ResponseEntity<?> importMovies(@RequestParam("file") MultipartFile file) {
        Map<String, Object> report = movieService.importExcel(file);
        return ResponseEntity.ok(report);
    }

    // 3. Lấy chi tiết phim
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<MovieDTO>> getMovieDetail(@PathVariable Long id) {
        MovieDTO movie = movieService.getMovieDetail(id);
        return ResponseEntity.ok(
                ApiResponse.<MovieDTO>builder()
                        .status(200)
                        .message("Lấy chi tiết phim thành công")
                        .data(movie)
                        .build()
        );
    }

    // 4. Thêm phim mới kèm Upload ảnh Poster và kho Ảnh Nhân vật (Admin/Super Admin)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Movie>> createMovie(
            @RequestPart("movie") MovieRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "characterFiles", required = false) List<MultipartFile> characterFiles
    ) {
        Movie movie = movieService.createMovie(request, file, characterFiles);
        return ResponseEntity.ok(
                ApiResponse.<Movie>builder()
                        .status(201)
                        .message("Thêm phim và upload ảnh thành công")
                        .data(movie)
                        .build()
        );
    }

    // 5. Cập nhật phim kèm đổi ảnh Poster và đổi kho Ảnh Nhân vật (Admin/Super Admin)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Movie>> updateMovie(
            @PathVariable Long id,
            @RequestPart("movie") MovieRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "characterFiles", required = false) List<MultipartFile> characterFiles
    ) {
        Movie movie = movieService.updateMovie(id, request, file, characterFiles);
        return ResponseEntity.ok(
                ApiResponse.<Movie>builder()
                        .status(200)
                        .message("Cập nhật phim thành công")
                        .data(movie)
                        .build()
        );
    }

    // 6. Xóa phim (Admin/Super Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(200)
                        .message("Xóa phim và file ảnh thành công")
                        .build()
        );
    }

    // 7. Lấy Top 3 Phim bán nhiều vé nhất (Public)
    @GetMapping("/top-tickets")
    public ResponseEntity<ApiResponse<List<com.example.cinema.dto.TopMovieTicketDTO>>> getTop3Movies() {
        return ResponseEntity.ok(
                ApiResponse.<List<com.example.cinema.dto.TopMovieTicketDTO>>builder()
                        .status(200)
                        .message("Lấy top 3 phim bán chạy thành công")
                        .data(movieService.getTop3MoviesByTickets())
                        .build()
        );
    }
}