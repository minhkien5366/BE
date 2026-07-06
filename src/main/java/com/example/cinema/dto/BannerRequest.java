package com.example.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.validation.constraints.Pattern;
@Data
public class BannerRequest {

    @NotBlank(message = "Tiêu đề không được để trống!")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự!")
    private String title;

    @NotBlank(message = "Link URL không được để trống!")
    @Size(max = 500, message = "Link URL tối đa 500 ký tự!")
    private String linkUrl;

    @NotBlank(message = "Trạng thái không được để trống!")
    @Pattern(
        regexp = "ACTIVE|INACTIVE",
        message = "Trạng thái chỉ được là ACTIVE hoặc INACTIVE"
    )
    private String status;
}