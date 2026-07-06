package com.example.cinema.service;

import com.example.cinema.dto.SeatRequest;
import com.example.cinema.entity.Seat;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;

public interface SeatService {
    List<Seat> getSeatsByShowtime(Long showtimeId);
    void validateSeatSelection(Long showtimeId, List<Long> selectedSeatIds);
    List<Seat> generateSeatsForRoom(Long roomId, int numRows, int seatsPerRow);
    Seat createSeat(SeatRequest request);
    Seat updateSeat(Long id, SeatRequest request);
    void deleteSeat(Long id);
    List<Seat> getSeatsByRoom(Long roomId);
    List<Seat> getAllSeats();
    void deleteSeatsByRoom(Long roomId);
    
    // Hàm mới phục vụ việc kiểm tra điều kiện xóa ghế từ Frontend
    Map<String, Boolean> checkSeatEligibility(Long id);

    // 🔥 API REAL-TIME: Khóa và nhả ghế ngay khi click
    void holdSeat(Long showtimeId, Long seatId, String userEmail);
    void releaseSeat(Long showtimeId, Long seatId, String userEmail);
}