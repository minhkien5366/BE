package com.example.cinema.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShowtimeRequest {
    private LocalDateTime startTime;
    private Long movieId;
    private Long cinemaItemId;
    private Long roomId;
}