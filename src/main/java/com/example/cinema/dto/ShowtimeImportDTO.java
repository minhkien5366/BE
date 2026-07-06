package com.example.cinema.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ShowtimeImportDTO {
    private String movieName;
    private String roomName;
    private LocalDateTime startTime;
}