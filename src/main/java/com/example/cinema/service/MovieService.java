package com.example.cinema.service;

import com.example.cinema.dto.MovieDTO;
import com.example.cinema.dto.MovieRequest;
import com.example.cinema.dto.TopMovieTicketDTO;
import com.example.cinema.entity.Movie;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.List;

public interface MovieService {
    Page<MovieDTO> getMovies(String search, String status, int page, int size);
    
    // 🎯 FIX LỖI Ở ĐÂY: Trả về DTO để Frontend lấy được reviewCount và genreNames
    MovieDTO getMovieDetail(Long id);
    
// Cập nhật lại 2 dòng này trong file MovieService.java
Movie createMovie(MovieRequest request, MultipartFile file, List<MultipartFile> characterFiles);
Movie updateMovie(Long id, MovieRequest request, MultipartFile file, List<MultipartFile> characterFiles);    void deleteMovie(Long id);
    Map<String, Object> importExcel(MultipartFile file);    
    List<TopMovieTicketDTO> getTop3MoviesByTickets();
}