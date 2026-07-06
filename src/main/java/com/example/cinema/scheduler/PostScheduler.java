package com.example.cinema.scheduler;

import com.example.cinema.entity.Post;
import com.example.cinema.dto.NewsResponse;
import com.example.cinema.service.NewsService;
import com.example.cinema.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PostScheduler {

    private final NewsService newsService;
    private final PostRepository postRepository;

    @Scheduled(fixedRate = 300000) // 5 phút
    public void autoImportNews() {

        System.out.println("Fetching movie news...");

        NewsResponse response = newsService.fetchMovieNews();

        if (response == null || response.getArticles() == null) return;

        response.getArticles().forEach(article -> {

            if (article.getTitle() == null) return;

            boolean exists =
                    postRepository.existsByTitle(article.getTitle());

            if (!exists) {

                Post post = Post.builder()
                        .title(article.getTitle())
                        .content(article.getDescription())
                        .thumbnail(article.getUrlToImage())
                        .published(true)
                        .createdAt(LocalDateTime.now())
                        .build();

                postRepository.save(post);

                System.out.println("🔥 IMPORTED: "
                        + article.getTitle());
            }
        });
    }
}