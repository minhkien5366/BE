package com.example.cinema.service.impl;

import com.example.cinema.dto.NewsResponse;
import com.example.cinema.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final RestTemplate restTemplate;

    private static final String API_KEY =
            "99a0e92b5e944650a1a22e65d66ea458";

    @Override
    public NewsResponse fetchMovieNews() {

        String url =
                "https://newsapi.org/v2/everything?q=movie&language=en&sortBy=publishedAt&apiKey="
                        + API_KEY;

        return restTemplate.getForObject(url, NewsResponse.class);
    }
}