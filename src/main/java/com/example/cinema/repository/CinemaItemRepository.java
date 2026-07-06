// ================= CinemaItemRepository.java =================
package com.example.cinema.repository;

import com.example.cinema.entity.CinemaItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CinemaItemRepository
        extends JpaRepository<CinemaItem, Long> {

    // ================= GET BY CINEMA =================
    List<CinemaItem> findByCinemaId(Long cinemaId);

    // ================= GET BY CITY =================
    List<CinemaItem> findByCity(String city);

    // ================= CHECK EXISTS =================
    boolean existsByCinema_Id(Long cinemaId);

    // ================= DELETE BY CINEMA =================
    void deleteByCinema_Id(Long cinemaId);

    // ================= CHECK TRÙNG TÊN =================
    boolean existsByNameIgnoreCaseAndCinema_Id(
            String name,
            Long cinemaId
    );

    // ================= CHECK TRÙNG KHI UPDATE =================
    boolean existsByNameIgnoreCaseAndCinema_IdAndIdNot(
            String name,
            Long cinemaId,
            Long id
    );
}