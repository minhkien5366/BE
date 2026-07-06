package com.example.cinema.repository;

import com.example.cinema.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    List<Combo> findByPriceLessThan(Double price);
    Optional<Combo> findByName(String name);
}