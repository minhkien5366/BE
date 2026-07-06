package com.example.cinema.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MovieRatingDTO {
    private String title;
    private Double avgRating;
    private Long count;
}