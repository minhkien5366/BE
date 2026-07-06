package com.example.cinema.service.impl;

import com.example.cinema.dto.ReviewRequest;
import com.example.cinema.entity.Movie;
import com.example.cinema.entity.Review;
import com.example.cinema.entity.User;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.MovieRepository;
import com.example.cinema.repository.ReviewRepository;
import com.example.cinema.repository.TicketRepository;
import com.example.cinema.repository.UserRepository;
import com.example.cinema.service.ReviewService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public Review createReview(ReviewRequest request) {

        User user = getCurrentUser();

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Phim không tồn tại"));

        if (request.getRating() < 0.5 || request.getRating() > 5.0) {
            throw new RuntimeException("Điểm đánh giá phải từ 0.5 đến 5.0.");
        }

        if (request.getComment() == null || request.getComment().trim().length() < 10) {
            throw new RuntimeException("Nội dung đánh giá phải có ít nhất 10 ký tự.");
        }

        boolean hasPaidTicket = ticketRepository.existsByUser_UserIdAndShowtime_Movie_IdAndStatus(
                user.getUserId(), movie.getId(), "PAID"
        );

        if (!hasPaidTicket) {
            throw new RuntimeException("Bạn cần mua vé để thực hiện đánh giá.");
        }

        boolean hasFinishedWatching = ticketRepository.existsByUser_UserIdAndShowtime_Movie_IdAndStatusAndShowtime_EndTimeBefore(
                user.getUserId(), movie.getId(), "PAID", LocalDateTime.now()
        );

        if (!hasFinishedWatching) {
            throw new RuntimeException("Vui lòng đợi phim chiếu xong để đánh giá.");
        }

        if (reviewRepository.existsByUser_UserIdAndMovieId(user.getUserId(), movie.getId())) {
            throw new RuntimeException("Bạn đã đánh giá bộ phim này rồi.");
        }

        Review review = Review.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .imageUrl(request.getImageUrl())
                .user(user)
                .movie(movie)
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);
        updateMovieRatingCache(movie);

        return savedReview;
    }

    // 🎯 THÊM MỚI: HÀM SỬA ĐÁNH GIÁ (Chỉ User mới được sửa bài của mình)
    @Override
    @Transactional
    public Review updateReview(Long reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá này"));

        User currentUser = getCurrentUser();

        // Kiểm tra chính chủ
        boolean isOwner = false;
        if (review.getUser() != null && review.getUser().getUserId() != null) {
            isOwner = review.getUser().getUserId().equals(currentUser.getUserId());
        }

        if (!isOwner) {
            throw new RuntimeException("Chỉ người viết mới có quyền sửa đánh giá này!");
        }

        if (request.getRating() < 0.5 || request.getRating() > 5.0) {
            throw new RuntimeException("Điểm đánh giá phải từ 0.5 đến 5.0.");
        }

        if (request.getComment() == null || request.getComment().trim().length() < 10) {
            throw new RuntimeException("Nội dung đánh giá phải có ít nhất 10 ký tự.");
        }

        // Cập nhật thông tin
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        
        // Chỉ cập nhật ảnh nếu user tải ảnh mới lên
        if (request.getImageUrl() != null) {
            review.setImageUrl(request.getImageUrl());
        }

        Review updatedReview = reviewRepository.save(review);
        updateMovieRatingCache(review.getMovie());

        return updatedReview;
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy đánh giá này"));

        User currentUser = getCurrentUser();

        boolean isSuperAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().toUpperCase().contains("SUPER_ADMIN"));

        boolean isOwner = false;
        if (review.getUser() != null && review.getUser().getUserId() != null) {
            isOwner = review.getUser().getUserId().equals(currentUser.getUserId());
        }

        // Cả Chủ sở hữu HOẶC Super Admin đều được phép xóa
        if (!isSuperAdmin && !isOwner) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này!");
        }

        Movie movie = review.getMovie();

        reviewRepository.delete(review);
        updateMovieRatingCache(movie);
    }

    @Override
    public List<Review> getReviewsByMovie(Long movieId) {
        return reviewRepository.findByMovieIdOrderByCreatedAtDesc(movieId);
    }

    private void updateMovieRatingCache(Movie movie) {

        Double avgRating = reviewRepository.getAverageRatingByMovieId(movie.getId());

        double roundedRating = (avgRating != null) ? Math.round(avgRating * 10.0) / 10.0 : 0.0;

        movie.setRating(roundedRating);
        movieRepository.save(movie);
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Phiên đăng nhập hết hạn"));
    }
}
