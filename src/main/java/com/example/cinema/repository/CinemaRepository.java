package com.example.cinema.repository;

import com.example.cinema.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long> {

    // ================= CHECK CREATE =================
    boolean existsByNameIgnoreCase(String name);

    // ================= CHECK UPDATE =================
    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );
}