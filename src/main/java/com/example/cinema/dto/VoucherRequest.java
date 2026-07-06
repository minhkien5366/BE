package com.example.cinema.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống!")
    private String code;

    @NotBlank(message = "Tiêu đề voucher không được để trống!")
    private String title;

    private String description;

    @NotNull(message = "Giá trị giảm không được để trống!")
    @Positive(message = "Giá trị giảm phải lớn hơn 0!")
    private Double discountValue;

    @PositiveOrZero(message = "Đơn hàng tối thiểu không được âm!")
    private Double minOrderAmount;

    @NotNull(message = "Số lượng mã không được để trống!")
    @Positive(message = "Số lượng voucher phải lớn hơn 0!")
    private Integer usageLimit;

    @NotNull(message = "Ngày bắt đầu không được để trống!")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống!")
    private LocalDateTime endDate;

    private Long promotionId;

    @Positive(message = "Điểm đổi phải lớn hơn 0!")
    private Integer costPoints;

    private String voucherType;

    @AssertTrue(message = "Ngày kết thúc phải sau ngày bắt đầu!")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
    @AssertTrue(message = "Giá trị giảm phải nhỏ hơn đơn hàng tối thiểu!")
    public boolean isValidDiscount() {
        if (discountValue == null || minOrderAmount == null) {
            return true;
        }
        return discountValue < minOrderAmount;
    }
}