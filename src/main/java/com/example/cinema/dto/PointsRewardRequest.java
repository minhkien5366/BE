package com.example.cinema.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointsRewardRequest {
    private String email;
    private Integer points;
}