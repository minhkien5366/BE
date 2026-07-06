// ================= RoomRequest.java =================
package com.example.cinema.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class RoomRequest {

    @NotBlank(message = "Tên phòng không được để trống")
    private String name;

    @NotNull(message = "Tổng số ghế không được để trống")
    @Min(value = 1, message = "Tổng số ghế phải lớn hơn 0")
    private Integer totalSeats;

    @NotNull(message = "Chi nhánh rạp không hợp lệ")
    private Long cinemaItemId;
}