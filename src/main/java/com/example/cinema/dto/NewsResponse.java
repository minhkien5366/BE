package com.example.cinema.dto;

import lombok.Data;
import java.util.List;

@Data
public class NewsResponse {

    private List<Article> articles;

    @Data
    public static class Article {
        private String title;
        private String description;
        private String urlToImage;
        private String publishedAt;
    }
}