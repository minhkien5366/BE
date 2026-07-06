package com.example.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenreRequest {

    @NotBlank(message = "Tên thể loại không được để trống")
    @Size(min = 2, max = 100, message = "Tên thể loại phải từ 2 - 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;
}