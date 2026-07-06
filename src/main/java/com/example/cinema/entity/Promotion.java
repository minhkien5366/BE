package com.example.cinema.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String thumbnail; // Ảnh đại diện sự kiện

    @ManyToOne
    @JoinColumn(name = "movie_id")
    @JsonIgnoreProperties("showtimes")
    private Movie movie; // Liên kết phim nếu là ưu đãi dành cho phim cụ thể

    @ManyToOne
    @JoinColumn(name = "cinema_item_id")
    @JsonIgnoreProperties({"rooms", "showtimes"})
    private CinemaItem cinemaItem; // NULL = Tin chung toàn hệ thống

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}