package com.example.cinema.service;

import com.example.cinema.dto.CinemaRequest;
import com.example.cinema.entity.Cinema;
import java.util.List;

public interface CinemaService {
    List<Cinema> getAllCinemas();
    
    // Bổ sung hàm lấy chi tiết cụm rạp
    Cinema getCinemaById(Long id);

    Cinema createCinema(CinemaRequest request);
    
    Cinema updateCinema(Long id, CinemaRequest request);
    
    void deleteCinema(Long id);
}