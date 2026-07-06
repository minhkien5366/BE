package com.example.cinema.service;

import com.example.cinema.dto.ReviewRequest;
import com.example.cinema.entity.Review;
import java.util.List;

public interface ReviewService {
    
   // Tạo đánh giá mới
    Review createReview(ReviewRequest request);

    // 🎯 THÊM MỚI: Cập nhật (Sửa) đánh giá
    Review updateReview(Long reviewId, ReviewRequest request);

    // Xóa đánh giá (Dành cho Owner và Super Admin)
    void deleteReview(Long reviewId);

    // Lấy danh sách đánh giá theo ID phim
    List<Review> getReviewsByMovie(Long movieId);

}
