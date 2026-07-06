package com.example.cinema.repository;

import com.example.cinema.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByCinemaItem_IdOrCinemaItemIsNull(Long cinemaItemId);
    List<Promotion> findByCinemaItem_Id(Long cinemaItemId);
    Optional<Promotion> findByTitle(String title);
    
}