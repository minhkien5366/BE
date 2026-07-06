package com.example.cinema.controller;

import com.example.cinema.entity.Post;
import com.example.cinema.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public List<Post> getLatest() {
        return postService.getLatestPosts();
    }

    @GetMapping("/{id}")
    public Post getDetail(@PathVariable Long id) {
        return postService.getPostDetail(id);
    }
}