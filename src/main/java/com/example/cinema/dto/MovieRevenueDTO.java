package com.example.cinema.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MovieRevenueDTO {
    private String movieName;
    private Double revenue;
}