package com.example.cinema.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự")
    // Nếu ông muốn bắt buộc mật khẩu phải khó (có chữ và số), có thể mở khóa dòng Pattern dưới đây:
    // @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{6,50}$", message = "Mật khẩu phải chứa ít nhất 1 chữ cái và 1 chữ số")
    private String password;

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

    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(
            regexp = "^(MALE|FEMALE|OTHER)$",
            message = "Giới tính chỉ được là MALE, FEMALE hoặc OTHER"
    )
    private String gender;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    private LocalDate dateOfBirth;
}