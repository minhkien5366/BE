package com.example.cinema.repository;

import com.example.cinema.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByStatus(String status);
    Optional<Banner> findByTitle(String title);
}