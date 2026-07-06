package com.example.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CinemaRequest {

    // ⭐ KHÔNG ĐƯỢC ĐỂ TRỐNG
    @NotBlank(message = "Tên rạp không được để trống")

    // ⭐ GIỚI HẠN ĐỘ DÀI
    @Size(
            min = 2,
            max = 100,
            message = "Tên rạp phải từ 2 đến 100 ký tự"
    )

    // ⭐ KHÔNG CHO NHẬP TOÀN KHOẢNG TRẮNG
    @Pattern(
            regexp = "^(?!\\s*$).+",
            message = "Tên rạp không hợp lệ"
    )
    private String name;
}