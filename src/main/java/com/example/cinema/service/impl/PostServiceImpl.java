package com.example.cinema.service.impl;

import com.example.cinema.entity.Post;
import com.example.cinema.repository.PostRepository;
import com.example.cinema.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    @Override
    public List<Post> getLatestPosts() {
        return postRepository.findTop10ByPublishedTrueOrderByCreatedAtDesc();
    }

    @Override
    public Post getPostDetail(Long id) {
        return postRepository.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    @Override
    public void createPost(Post post) {
        postRepository.save(post);
    }
}