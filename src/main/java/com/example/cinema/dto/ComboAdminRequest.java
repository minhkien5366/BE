package com.example.cinema.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComboAdminRequest {

    @NotNull(message = "Số lượng tồn kho không được để trống!")
    @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0!")
    private Integer stock;
}