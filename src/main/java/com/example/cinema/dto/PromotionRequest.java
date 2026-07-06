package com.example.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PromotionRequest {

    @NotBlank(message = "Tên khuyến mãi không được để trống!")
    @Size(min = 5, max = 150, message = "Tên khuyến mãi phải từ 5 - 150 ký tự!")
    private String title;

    @NotBlank(message = "Nội dung khuyến mãi không được để trống!")
    @Size(min = 10, max = 5000, message = "Nội dung phải từ 10 - 5000 ký tự!")
    private String content;

    private Long movieId;

    private Long cinemaItemId;
}