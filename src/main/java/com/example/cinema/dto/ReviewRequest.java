package com.example.cinema.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long movieId;
    private Double rating; // FIX: Đổi từ Integer sang Double để nhận điểm nửa sao (vd: 4.5)
    private String comment;
    private String imageUrl;
}