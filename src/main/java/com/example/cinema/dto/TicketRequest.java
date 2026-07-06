package com.example.cinema.dto;

import lombok.Data;

@Data
public class TicketRequest {
    private Long seatId;
    private Long showtimeId;
}