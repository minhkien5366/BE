package com.example.cinema.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SeatPriceConfigRequest {

    @NotBlank(message = "Loại ghế không được để trống!")
    @Pattern(
            regexp = "NORMAL|VIP|COUPLE",
            message = "Loại ghế chỉ được là NORMAL, VIP hoặc COUPLE"
    )
    private String seatType;

    @NotNull(message = "Ngày trong tuần không được để trống!")
    @Min(value = 2, message = "Ngày phải từ 2 đến 8")
    @Max(value = 8, message = "Ngày phải từ 2 đến 8")
    private Integer dayOfWeek;

    @NotNull(message = "Giá tiền không được để trống!")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Giá tiền phải lớn hơn 0"
    )
    private Double price;
}