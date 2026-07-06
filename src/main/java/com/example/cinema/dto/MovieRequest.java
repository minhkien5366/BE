package com.example.cinema.dto;



import lombok.Data;

import java.time.LocalDate;

import java.util.List;



@Data

public class MovieRequest {

    private String title;

    private String description;

    private Integer duration;

    private String director;

    private String cast;

    private String country;

    private String status;

    private String trailerUrl;

    private LocalDate releaseDate;

    

    // THAY ĐỔI: Chuyển từ Integer genreId sang List<Integer> genreIds

    private List<Integer> genreIds; 

    

    private String ageRating;



    // 🔥 THÊM MỚI: Biến hứng danh sách link ảnh cũ nếu Admin đang Edit phim mà không chọn ảnh nhân vật mới

    private String existingCharacterImages; 

}