package com.example.cinema.controller;

import com.example.cinema.dto.MovieRevenueDTO;
import com.example.cinema.service.FinanceService;
import com.example.cinema.service.ReportService;
import com.example.cinema.service.impl.ReviewServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;
    @Autowired
    private ReviewServiceImpl reviewService;
    @Autowired
    private FinanceService financeService;


    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
        @GetMapping("/download")
        public ResponseEntity<InputStreamResource> download(
                @RequestParam(required = false) Long cinemaId,
                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start, 
                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                @RequestParam(required = false) Double taxRate 
        ) throws IOException {

            var inputStream = reportService.exportRevenueReport(cinemaId, start, end, taxRate);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BaoCaoDoanhThu.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(inputStream));
        }
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @GetMapping("/ranking")
    public ResponseEntity<?> getRanking(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return ResponseEntity.ok(reportService.getCinemaRankingData(start, end));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
        @GetMapping("/stats")
        public ResponseEntity<?> getMovieStats() {
            return ResponseEntity.ok(reportService.getMovieStatistics());
        }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(
            @RequestParam(required = false) Long cinemaId
    ) {
        return ResponseEntity.ok(
                reportService.getAdminDashboard(cinemaId)
        );
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue-7days")
    public ResponseEntity<?> chart(@RequestParam Long cinemaId) {

        return ResponseEntity.ok(
                reportService.getAdminRevenue7Days(cinemaId)
        );
    }
@PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/finance")
    public ResponseEntity<?> getFinanceReport(
            @RequestParam String month,
            @RequestParam(defaultValue = "10.0") Double taxRate) { // Nhận thêm phần trăm thuế, mặc định 10%
        
        return ResponseEntity.ok(financeService.getMonthlyFinance(month, taxRate));
    }
    @GetMapping("/combo-best-selling")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<?> getBestSellingCombos(
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end
    ) {

        if (start == null || end == null) {
            LocalDateTime now = LocalDateTime.now();
            start = now.toLocalDate().atStartOfDay();      // 00:00
            end = now.toLocalDate().atTime(23, 59, 59);    // 23:59
        }

        return ResponseEntity.ok(
                reportService.getBestSellingCombos(cinemaId, start, end)
        );
    }

    @GetMapping("/movie-revenue")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getMovieRevenue(
            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime startDate,

            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime endDate) {

        return ResponseEntity.ok(
                reportService.getMovieRevenue(startDate, endDate)
        );
    }
}