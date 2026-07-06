package com.example.cinema.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "Họ không được để trống")
    @Size(min = 2, max = 50, message = "Họ phải từ 2 đến 50 ký tự")
    @Pattern(
            regexp = "^[\\p{L}\\s]+$",
            message = "Họ không được chứa số hoặc ký tự đặc biệt"
    )
    private String firstName;

    @NotBlank(message = "Tên không được để trống")
    @Size(min = 2, max = 50, message = "Tên phải từ 2 đến 50 ký tự")
    @Pattern(
            regexp = "^[\\p{L}\\s]+$",
            message = "Tên không được chứa số hoặc ký tự đặc biệt"
    )
    private String lastName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^(0|\\+84)[0-9]{9,10}$",
            message = "Số điện thoại không hợp lệ (phải bắt đầu bằng 0 hoặc +84 và có 10-11 chữ số)"
    )
    private String mobileNumber;

    // Bổ sung @NotNull vì @Past vẫn sẽ cho lọt nếu dữ liệu gửi lên là null
    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(
            // Thêm dấu ^ và $ để khóa chặt chuỗi, tránh việc nhập "MALE123" vẫn hợp lệ
            regexp = "^(MALE|FEMALE|OTHER)$",
            message = "Giới tính chỉ được là MALE, FEMALE hoặc OTHER"
    )
    private String gender;
}