package com.example.cinema.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComboAdminResponse {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Double price;
    private boolean isAvailable;
    private Integer stock;
}