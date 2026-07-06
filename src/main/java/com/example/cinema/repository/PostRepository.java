package com.example.cinema.repository;

import com.example.cinema.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByTitle(String title);

    List<Post> findTop10ByPublishedTrueOrderByCreatedAtDesc();
    Optional<Post> findByIdAndPublishedTrue(Long id);
}