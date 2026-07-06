package com.example.cinema.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.cinema.dto.AdminDashboardDTO;
import com.example.cinema.dto.ComboReportResponse;
import com.example.cinema.dto.MovieRatingDTO;
import com.example.cinema.dto.MovieRevenueDTO;
import com.example.cinema.dto.RevenueChartDTO;
public interface ReportService {
    ByteArrayInputStream exportRevenueReport(Long cinemaId, LocalDateTime start, LocalDateTime end, Double taxRateInput) throws IOException;    List<Map<String, Object>> getCinemaRankingData(LocalDateTime start, LocalDateTime end);
    List<MovieRatingDTO> getMovieStatistics();
    AdminDashboardDTO getAdminDashboard(Long cinemaId);
    List<RevenueChartDTO> getAdminRevenue7Days(Long cinemaId);
    List<ComboReportResponse> getBestSellingCombos(Long cinemaId, LocalDateTime start, LocalDateTime end);
    List<MovieRevenueDTO> getMovieRevenue(
        LocalDateTime startDate,
        LocalDateTime endDate);
}