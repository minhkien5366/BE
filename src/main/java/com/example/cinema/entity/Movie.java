package com.example.cinema.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movies")
@Data
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
    
    private Integer duration;
    
    private String director;

    @Column(columnDefinition = "TEXT")
    private String cast;

    // 🎯 THAY ĐỔI: Chuyển từ @ManyToOne sang @ManyToMany
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "movie_genre", // Tên bảng trung gian
        joinColumns = @JoinColumn(name = "movie_id"), // Khóa ngoại trỏ đến bảng movies
        inverseJoinColumns = @JoinColumn(name = "genre_id") // Khóa ngoại trỏ đến bảng genres
    )
    @ToString.Exclude     // Tránh bị vòng lặp vô hạn khi log/print (do Lombok)
    @EqualsAndHashCode.Exclude // Tránh lỗi tuần hoàn khi so sánh hashcode
    private Set<Genre> genres = new HashSet<>();

    private Double rating;
    private String country;
    private String status;
    private String posterUrl;
    private String trailerUrl;
    private LocalDate releaseDate;
    
    // Phân loại độ tuổi (P, K, T13, T16, T18, C)
    @Column(length = 10)
    private String ageRating;

    // 🔥 THÊM MỚI Ở ĐÂY: Lưu các link ảnh nhân vật phim do Admin chọn, cách nhau bằng dấu phẩy
    @Column(columnDefinition = "TEXT")
    private String characterImages;

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}