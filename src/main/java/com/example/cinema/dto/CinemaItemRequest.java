// ================= CinemaItemRequest.java =================
package com.example.cinema.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class CinemaItemRequest {

    @NotBlank(message = "Tên chi nhánh không được để trống")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotBlank(message = "Thành phố không được để trống")
    private String city;

    @NotNull(message = "Số giờ hoạt động không được để trống")
    @Min(value = 1, message = "Số giờ hoạt động phải lớn hơn 0")
    private Integer hoursPerRoom;

    @NotNull(message = "Cụm rạp không hợp lệ")
    private Long cinemaId;
}