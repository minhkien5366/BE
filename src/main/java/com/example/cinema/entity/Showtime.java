package com.example.cinema.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "cinema_item_id")
    @JsonIgnoreProperties("showtimes")
    private CinemaItem cinemaItem;

    @ManyToOne
    @JoinColumn(name = "movie_id")
    @JsonIgnoreProperties("showtimes")
    private Movie movie;

    @ManyToOne
    @JoinColumn(
            name = "room_id",
            nullable = true
    )
    @JsonIgnoreProperties("showtimes")
    private Room room;

    // 🔥 THÊM TRƯỜNG TRẠNG THÁI VÀ LÝ DO ĐỂ DUYỆT HỦY VÉ
    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // Trạng thái: ACTIVE, PENDING_CANCEL, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String cancelReason; // Lý do xin hủy
}