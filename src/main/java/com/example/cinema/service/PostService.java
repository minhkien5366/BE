package com.example.cinema.service;

import com.example.cinema.entity.Post;
import java.util.List;

public interface PostService {

    List<Post> getLatestPosts();

    Post getPostDetail(Long id);

    void createPost(Post post);
}