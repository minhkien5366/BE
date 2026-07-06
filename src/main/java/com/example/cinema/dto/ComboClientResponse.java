package com.example.cinema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // 🔥 BẮT BUỘC PHẢI CÓ: Dòng này sẽ xóa sạch lỗi `.builder() undefined`
@AllArgsConstructor
@NoArgsConstructor
public class ComboClientResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Double price;
    private Integer stock; 
}