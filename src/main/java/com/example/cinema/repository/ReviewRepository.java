package com.example.cinema.repository;

import com.example.cinema.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // ⭐ LẤY DANH SÁCH REVIEW THEO PHIM
    List<Review> findByMovieIdOrderByCreatedAtDesc(Long movieId);

    // ⭐ TÍNH ĐIỂM TRUNG BÌNH CỦA PHIM
    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.movie.id = :movieId
    """)
    Double getAverageRatingByMovieId(
            @Param("movieId") Long movieId
    );

    // ⭐ ĐẾM TỔNG LƯỢT ĐÁNH GIÁ
    @Query("""
        SELECT COUNT(r)
        FROM Review r
        WHERE r.movie.id = :movieId
    """)
    Long countReviewsByMovieId(
            @Param("movieId") Long movieId
    );

    // ⭐ KIỂM TRA USER ĐÃ REVIEW CHƯA
    boolean existsByUser_UserIdAndMovieId(
            Long userId,
            Long movieId
    );

    // ⭐ TOP PHIM ĐIỂM CAO NHẤT
    @Query("""
        SELECT
            r.movie.title,
            AVG(r.rating),
            COUNT(r.id)
        FROM Review r
        GROUP BY r.movie.id, r.movie.title
        ORDER BY AVG(r.rating) DESC
    """)
    List<Object[]> getTopRatedMovies();
}