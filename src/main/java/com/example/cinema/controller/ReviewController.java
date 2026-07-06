package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.ReviewRequest;
import com.example.cinema.entity.Review;
import com.example.cinema.service.CloudinaryService;
import com.example.cinema.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final CloudinaryService cloudinaryService;

    // ⭐ TẠO ĐÁNH GIÁ (CREATE)
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<Review>> createReview(
            @RequestParam("rating") Double rating,
            @RequestParam("comment") String comment,
            @RequestParam("movieId") Long movieId,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(image, "reviews");
        }

        ReviewRequest request = new ReviewRequest();
        request.setRating(rating);
        request.setComment(comment);
        request.setMovieId(movieId);
        request.setImageUrl(imageUrl); 

        return ResponseEntity.ok(ApiResponse.<Review>builder()
                .status(201)
                .message("Đánh giá thành công, cảm ơn bạn!")
                .data(reviewService.createReview(request))
                .build());
    }

    // ⭐ SỬA ĐÁNH GIÁ (UPDATE) - Dành cho User
    @PutMapping(value = "/{reviewId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<Review>> updateReview(
            @PathVariable Long reviewId,
            @RequestParam("rating") Double rating,
            @RequestParam("comment") String comment,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(image, "reviews");
        }

        ReviewRequest request = new ReviewRequest();
        request.setRating(rating);
        request.setComment(comment);
        request.setImageUrl(imageUrl); 

        return ResponseEntity.ok(ApiResponse.<Review>builder()
                .status(200)
                .message("Cập nhật đánh giá thành công!")
                .data(reviewService.updateReview(reviewId, request))
                .build());
    }

    // ⭐ LẤY DANH SÁCH ĐÁNH GIÁ THEO PHIM (READ)
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<List<Review>>> getByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.<List<Review>>builder()
                .status(200)
                .data(reviewService.getReviewsByMovie(movieId))
                .build());
    }

    // ⭐ XÓA ĐÁNH GIÁ (DELETE) - Dành cho User và Super Admin
    // Đã gỡ bỏ @PreAuthorize để Service tự check quyền
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<String>> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(200)
                        .message("Đã xóa đánh giá thành công!")
                        .build()
        );
    }
}


