package com.example.cinema.repository;

import com.example.cinema.entity.CinemaCombo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CinemaComboRepository
        extends JpaRepository<CinemaCombo, Long> {

    Optional<CinemaCombo>
    findByCinemaItemIdAndComboId(Long cinemaItemId, Long comboId);

    List<CinemaCombo> findByCinemaItemId(Long cinemaItemId);
            
    List<CinemaCombo>
    findByCinemaItemIdAndActiveFalse(Long cinemaItemId);
    
}