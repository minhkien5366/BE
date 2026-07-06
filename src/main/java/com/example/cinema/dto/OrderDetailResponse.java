package com.example.cinema.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private Long id;
    private String itemType;
    private Long itemId;
    private Integer quantity;
    private Double price;
    private String itemName;
}