package com.example.cinema.dto;

public interface TopMovieTicketDTO {
    Long getMovieId();
    String getTitle();
    String getPosterUrl();
    Long getTotalTickets(); // Tổng số vé bán được của phim này
}