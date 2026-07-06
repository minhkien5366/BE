package com.example.cinema.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ComboRequest {

    @NotBlank(message = "Tên combo không được để trống!")
    @Size(min = 2, max = 255, message = "Tên combo từ 2 - 255 ký tự!")
    private String name;

    @NotBlank(message = "Mô tả combo không được để trống!")
    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự!")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá combo phải lớn hơn 0!")
    private Double price;
}