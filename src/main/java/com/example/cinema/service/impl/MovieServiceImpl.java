package com.example.cinema.service.impl;

import com.example.cinema.dto.MovieDTO;
import com.example.cinema.dto.MovieRequest;
import com.example.cinema.dto.TopMovieTicketDTO;
import com.example.cinema.entity.Genre;
import com.example.cinema.entity.Movie;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.GenreRepository;
import com.example.cinema.repository.MovieRepository;
import com.example.cinema.repository.ReviewRepository;
import com.example.cinema.repository.TicketRepository;
import com.example.cinema.service.CloudinaryService;
import com.example.cinema.service.MovieService;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final CloudinaryService cloudinaryService;
    private final TicketRepository ticketRepository;
    private final ReviewRepository reviewRepository;

    private static class RowMovieImportResult {
        int rowIndex;
        String title;
        String genres;
        String statusStr; 
        String status;    
        String errorMessage;

        RowMovieImportResult(int rowIndex, String title, String genres, String statusStr, String status, String errorMessage) {
            this.rowIndex = rowIndex;
            this.title = title;
            this.genres = genres;
            this.statusStr = statusStr;
            this.status = status;
            this.errorMessage = errorMessage;
        }
    }

    @Override
    public Page<MovieDTO> getMovies(String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Movie> moviePage;

        if (search != null && !search.isEmpty()) {
            moviePage = movieRepository.findByTitleContainingIgnoreCase(search, pageable);
        } else if (status != null && !status.isEmpty()) {
            moviePage = movieRepository.findByStatus(status, pageable);
        } else {
            moviePage = movieRepository.findAll(pageable);
        }

        List<MovieDTO> dtos = moviePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, moviePage.getTotalElements());
    }

    @Override
    public MovieDTO getMovieDetail(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim với ID: " + id));
        return convertToDTO(movie);
    }

    @Override
    @Transactional
    public Movie createMovie(MovieRequest request, MultipartFile file, List<MultipartFile> characterFiles) {
        Set<Genre> genres = new HashSet<>(genreRepository.findAllById(request.getGenreIds()));
        if (genres.isEmpty()) {
            throw new ResourceNotFoundException("Danh sách thể loại không hợp lệ hoặc trống!");
        }

        Movie movie = new Movie();
        mapRequestToEntity(request, movie, genres);

        // Upload Poster chính
        if (file != null && !file.isEmpty()) {
            try {
                String url = cloudinaryService.uploadImage(file, "movies");
                movie.setPosterUrl(url);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload ảnh lên Cloudinary: " + e.getMessage());
            }
        }

        // 🔥 XỬ LÝ UPLOAD HÀNG LOẠT ẢNH NHÂN VẬT LÊN CLOUDINARY
        if (characterFiles != null && !characterFiles.isEmpty()) {
            List<String> uploadedUrls = new ArrayList<>();
            for (MultipartFile charFile : characterFiles) {
                if (!charFile.isEmpty()) {
                    try {
                        String charUrl = cloudinaryService.uploadImage(charFile, "characters");
                        uploadedUrls.add(charUrl);
                    } catch (IOException e) {
                        System.err.println("Lỗi tải ảnh nhân vật: " + e.getMessage());
                    }
                }
            }
            if (!uploadedUrls.isEmpty()) {
                // Ghép mảng URL thành một chuỗi, cách nhau bởi dấu phẩy
                movie.setCharacterImages(String.join(",", uploadedUrls));
            }
        }

        return movieRepository.save(movie);
    }

    @Override
    @Transactional
    public Movie updateMovie(Long id, MovieRequest request, MultipartFile file, List<MultipartFile> characterFiles) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim để cập nhật"));

        Set<Genre> genres = new HashSet<>(genreRepository.findAllById(request.getGenreIds()));
        if (genres.isEmpty()) {
            throw new ResourceNotFoundException("Danh sách thể loại không hợp lệ hoặc trống!");
        }

        mapRequestToEntity(request, movie, genres);

        // Upload Poster chính
        if (file != null && !file.isEmpty()) {
            try {
                if (movie.getPosterUrl() != null && movie.getPosterUrl().contains("cloudinary")) {
                    cloudinaryService.deleteImage(movie.getPosterUrl());
                }
                String url = cloudinaryService.uploadImage(file, "movies");
                movie.setPosterUrl(url);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi xử lý ảnh Cloudinary: " + e.getMessage());
            }
        }

        // 🔥 XỬ LÝ ẢNH NHÂN VẬT KHI CẬP NHẬT
        if (characterFiles != null && !characterFiles.isEmpty()) {
            // Bước 1: Xóa đống ảnh nhân vật cũ trên mây (nếu có) để dọn rác
            if (movie.getCharacterImages() != null && !movie.getCharacterImages().trim().isEmpty()) {
                String[] oldImages = movie.getCharacterImages().split(",");
                for (String oldImg : oldImages) {
                    try {
                        if (oldImg.contains("cloudinary")) cloudinaryService.deleteImage(oldImg.trim());
                    } catch (Exception e) {}
                }
            }
            
            // Bước 2: Upload đống ảnh mới
            List<String> uploadedUrls = new ArrayList<>();
            for (MultipartFile charFile : characterFiles) {
                if (!charFile.isEmpty()) {
                    try {
                        String charUrl = cloudinaryService.uploadImage(charFile, "characters");
                        uploadedUrls.add(charUrl);
                    } catch (IOException e) {}
                }
            }
            movie.setCharacterImages(String.join(",", uploadedUrls));
        } else if (request.getExistingCharacterImages() != null) {
            // Nếu người dùng không đưa file mới, mà cố ý muốn giữ file cũ thông qua param
            movie.setCharacterImages(request.getExistingCharacterImages());
        }

        return movieRepository.save(movie);
    }

    @Override
    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim để xóa"));

        // Xóa Poster
        if (movie.getPosterUrl() != null && movie.getPosterUrl().contains("cloudinary")) {
            try {
                cloudinaryService.deleteImage(movie.getPosterUrl());
            } catch (IOException e) {
                System.err.println("Lỗi xóa ảnh trên Cloud: " + e.getMessage());
            }
        }

        // Xóa Ảnh nhân vật trên Cloudinary để giải phóng dung lượng
        if (movie.getCharacterImages() != null && !movie.getCharacterImages().trim().isEmpty()) {
            String[] charImages = movie.getCharacterImages().split(",");
            for (String charImg : charImages) {
                try {
                    if (charImg.contains("cloudinary")) cloudinaryService.deleteImage(charImg.trim());
                } catch (Exception e) {}
            }
        }

        movie.getGenres().clear();
        movieRepository.delete(movie);
    }

    @Override
    public List<TopMovieTicketDTO> getTop3MoviesByTickets() {
        Pageable top3 = PageRequest.of(0, 3);
        return ticketRepository.findTopMoviesByTicketSales(top3);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) 
    public Map<String, Object> importExcel(MultipartFile file) {
        Map<String, Object> resultReport = new HashMap<>();
        List<Map<String, Object>> formattedRowDetails = new ArrayList<>();
        List<RowMovieImportResult> tempResults = new ArrayList<>();
        boolean hasAnyError = false;

        Set<String> duplicateTitleChecker = new HashSet<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[M/d/yyyy][yyyy-MM-dd]");
            int totalRows = sheet.getLastRowNum();

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String title = readString(row.getCell(0));
                String rawStatus = readString(row.getCell(6));
                String genreNamesData = readString(row.getCell(8));

                if (title == null || title.isBlank()) continue;

                try {
                    Integer duration = readInt(row.getCell(2));

                    if (duration == null || genreNamesData == null || genreNamesData.isBlank()) {
                        throw new RuntimeException("Thiếu dữ liệu bắt buộc (Tiêu đề, Thời lượng, Thể loại)");
                    }

                    if (movieRepository.existsByTitleIgnoreCase(title.trim())) {
                        throw new RuntimeException("Phim '" + title.trim() + "' đã tồn tại sẵn trong hệ thống");
                    }

                    String lowerTitle = title.trim().toLowerCase();
                    if (duplicateTitleChecker.contains(lowerTitle)) {
                        throw new RuntimeException("Phim '" + title.trim() + "' bị lặp lại tiêu đề ngay trong tệp Excel này");
                    }
                    duplicateTitleChecker.add(lowerTitle);

                    String[] genreSplit = genreNamesData.split("[,;]");
                    for (String gName : genreSplit) {
                        String cleanName = gName.trim();
                        if (cleanName.isEmpty()) continue;
                        if (!genreRepository.existsByNameIgnoreCase(cleanName)) {
                            throw new RuntimeException("Không tìm thấy danh mục thể loại '" + cleanName + "'");
                        }
                    }

                    if (rawStatus != null && !rawStatus.isBlank()) {
                        String cleanStatus = rawStatus.trim().toLowerCase();
                        if (!cleanStatus.contains("đang chiếu") && !cleanStatus.contains("sắp chiếu") && 
                            !cleanStatus.contains("ngừng chiếu") && !cleanStatus.contains("đã chiếu")) {
                            throw new RuntimeException("Trạng thái '" + rawStatus + "' không hợp lệ (Chỉ chấp nhận: Đang chiếu, Sắp chiếu, Ngừng chiếu)");
                        }
                    }

                    tempResults.add(new RowMovieImportResult(i + 1, title, genreNamesData, rawStatus, "SUCCESS", null));

                } catch (Exception rowError) {
                    tempResults.add(new RowMovieImportResult(i + 1, title, genreNamesData, rawStatus, "FAILED", rowError.getMessage()));
                    hasAnyError = true; 
                }
            }

            for (RowMovieImportResult res : tempResults) {
                Map<String, Object> detailMap = new HashMap<>();
                detailMap.put("rowIndex", res.rowIndex);
                detailMap.put("title", res.title);
                detailMap.put("genres", res.genres);
                detailMap.put("statusStr", res.statusStr);
                detailMap.put("status", res.status);
                
                if (hasAnyError && "SUCCESS".equals(res.status)) {
                    detailMap.put("errorMessage", "Bị hủy lưu do có phim khác trong file dính lỗi.");
                } else {
                    detailMap.put("errorMessage", res.errorMessage);
                }
                
                formattedRowDetails.add(detailMap);
            }

            if (hasAnyError) {
                resultReport.put("isSuccess", false);
                resultReport.put("rowDetails", formattedRowDetails);
                return resultReport; 
            }

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String titleCheck = readString(row.getCell(0));
                if (titleCheck == null || titleCheck.isBlank()) continue;

                saveIndividualMovie(row, formatter);
            }

            resultReport.put("isSuccess", true);
            resultReport.put("rowDetails", formattedRowDetails);
            return resultReport;

        } catch (Exception e) {
            throw new RuntimeException("Import thất bại do lỗi hệ thống: " + e.getMessage(), e);
        }
    }
    
    public void saveIndividualMovie(Row row, DateTimeFormatter formatter) {
        String title = readString(row.getCell(0));
        String description = readString(row.getCell(1));
        Integer duration = readInt(row.getCell(2));
        String director = readString(row.getCell(3));
        String cast = readString(row.getCell(4));
        String country = readString(row.getCell(5));
        String rawStatus = readString(row.getCell(6)); 
        LocalDate releaseDate = readDate(row.getCell(7), formatter);
        String genreNamesData = readString(row.getCell(8));
        String posterUrl = readString(row.getCell(9));
        String trailerUrl = readString(row.getCell(10));
        String ageRating = readString(row.getCell(11));

        if (title == null || title.isBlank() || duration == null || genreNamesData == null || genreNamesData.isBlank()) {
            throw new RuntimeException("Thiếu dữ liệu bắt buộc (Tiêu đề, Thời lượng, Thể loại)");
        }

        if (movieRepository.existsByTitleIgnoreCase(title)) {
            throw new RuntimeException("Phim '" + title + "' đã tồn tại trong hệ thống");
        }

        Set<Genre> genres = new HashSet<>();
        String[] genreSplit = genreNamesData.split("[,;]");
        for (String gName : genreSplit) {
            String cleanName = gName.trim();
            if (cleanName.isEmpty()) continue;

            Genre genre = genreRepository.findByNameIgnoreCase(cleanName)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục thể loại '" + cleanName + "'"));
            genres.add(genre);
        }

        if (genres.isEmpty()) {
            throw new RuntimeException("Không có thể loại nào hợp lệ");
        }

        String dbStatus = "SHOWING";
        if (rawStatus != null && !rawStatus.isBlank()) {
            String cleanStatus = rawStatus.trim().toLowerCase();
            if (cleanStatus.contains("đang chiếu")) {
                dbStatus = "SHOWING";
            } else if (cleanStatus.contains("sắp chiếu")) {
                dbStatus = "COMING_SOON";
            } else if (cleanStatus.contains("ngừng chiếu") || cleanStatus.contains("đã chiếu")) {
                dbStatus = "ENDED";
            } else {
                throw new RuntimeException("Trạng thái '" + rawStatus + "' không hợp lệ (Chỉ chấp nhận: Đang chiếu, Sắp chiếu, Ngừng chiếu)");
            }
        }

        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setDuration(duration);
        movie.setDirector(director);
        movie.setCast(cast);
        movie.setCountry(country);
        movie.setStatus(dbStatus); 
        movie.setReleaseDate(releaseDate);
        movie.setPosterUrl(posterUrl);
        movie.setTrailerUrl(trailerUrl);
        movie.setAgeRating(ageRating != null && !ageRating.isBlank() ? ageRating : "P");
        
        movie.setGenres(genres);
        movieRepository.save(movie);
    }
    
    private MovieDTO convertToDTO(Movie movie) {
        MovieDTO dto = new MovieDTO();
        
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setDescription(movie.getDescription());
        dto.setDirector(movie.getDirector());
        dto.setCast(movie.getCast());
        dto.setCountry(movie.getCountry());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setTrailerUrl(movie.getTrailerUrl());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setDuration(movie.getDuration());
        dto.setStatus(movie.getStatus());
        dto.setRating(movie.getRating());
        dto.setAgeRating(movie.getAgeRating());
        dto.setCreatedAt(movie.getCreatedAt());
        dto.setUpdatedAt(movie.getUpdatedAt());

        // 🔥 GẮN LINK ẢNH NHÂN VẬT RA CHO FRONTEND DỄ HIỂN THỊ TRONG MÀN QUẢN LÝ
        if (movie.getCharacterImages() != null && !movie.getCharacterImages().trim().isEmpty()) {
            List<String> charImgs = Arrays.asList(movie.getCharacterImages().split(","));
            dto.setCharacterImages(charImgs);
        }

        Long reviewCount = reviewRepository.countReviewsByMovieId(movie.getId());
        dto.setReviewCount(reviewCount);

        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            Set<String> genreNames = movie.getGenres().stream()
                    .map(Genre::getName)
                    .collect(Collectors.toSet());
            dto.setGenreNames(genreNames);
        }

        return dto;
    }

    private void mapRequestToEntity(MovieRequest request, Movie movie, Set<Genre> genres) {
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setDuration(request.getDuration());
        movie.setDirector(request.getDirector());
        movie.setCast(request.getCast());
        movie.setCountry(request.getCountry());
        movie.setStatus(request.getStatus());
        movie.setTrailerUrl(request.getTrailerUrl());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setAgeRating(request.getAgeRating());
        
        movie.setGenres(genres); 
    }

    private String readString(Cell cell) {
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private Integer readInt(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        try {
            return Integer.parseInt(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate readDate(Cell cell, DateTimeFormatter formatter) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        return LocalDate.parse(cell.getStringCellValue().trim(), formatter);
    }
}