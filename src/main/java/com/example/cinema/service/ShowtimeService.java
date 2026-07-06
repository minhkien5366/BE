package com.example.cinema.service;

import com.example.cinema.dto.ShowtimeRequest;
import com.example.cinema.entity.Showtime;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ShowtimeService {
    List<Showtime> getAll();
    Showtime getById(Long id);
    List<Showtime> getByMovie(Long movieId);
    List<Showtime> getByCinemaItem(Long cinemaItemId);
    List<Showtime> getByMovieAndDate(Long movieId, String dateStr);
    
    Showtime createShowtime(ShowtimeRequest request);
    Showtime updateShowtime(Long id, ShowtimeRequest request);
    void deleteShowtime(Long id);
    void importExcel(MultipartFile file);

    // 🔥 3 HÀM MỚI CHO QUY TRÌNH DUYỆT HỦY
    Showtime requestCancel(Long id, String reason);
    void approveCancel(Long id);
    Showtime rejectCancel(Long id);
}