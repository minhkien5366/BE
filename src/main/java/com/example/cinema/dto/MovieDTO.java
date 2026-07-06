package com.example.cinema.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List; // 🔥 Đã import thư viện List

@Data
public class MovieDTO {
    private Long id;
    private String title;
    
    // 🎯 THÊM ĐẦY ĐỦ CÁC TRƯỜNG CỦA PHIM
    private String description;
    private String director;
    private String cast;
    private String country;
    private LocalDate releaseDate;
    private String trailerUrl;
    
    private String posterUrl;
    private Integer duration;
    
    // 🎯 Danh sách tên thể loại (Hỗ trợ Nhiều-Nhiều)
    private Set<String> genreNames;
    
    private String status;
    private Double rating;

    // ĐỘ TUỔI
    private String ageRating;

    // TỔNG LƯỢT ĐÁNH GIÁ
    private Long reviewCount;

    // 🔥 THÊM MỚI Ở ĐÂY: Danh sách ảnh nhân vật để gửi ra Frontend
    private List<String> characterImages;

    // THỜI GIAN TẠO & CẬP NHẬT
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
}