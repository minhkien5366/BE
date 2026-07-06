// ================= RoomRepository.java =================
package com.example.cinema.repository;

import com.example.cinema.entity.Room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository
        extends JpaRepository<Room, Long> {

    // ================= GET BY CINEMA ITEM =================
    List<Room> findByCinemaItem_Id(
            Long cinemaItemId
    );

    // ================= FIND BY NAME =================
    Optional<Room> findByName(
            String name
    );

    // ================= FIND BY NAME + CINEMA =================
    Optional<Room> findByNameAndCinemaItem_Id(
            String name,
            Long cinemaItemId
    );

    // ================= CHECK EXISTS =================
    boolean existsByCinemaItem_Id(
            Long cinemaItemId
    );

    // ================= DELETE BY CINEMA =================
    void deleteByCinemaItem_Id(
            Long cinemaItemId
    );

    // ================= CHECK DUPLICATE CREATE =================
    boolean existsByNameIgnoreCaseAndCinemaItem_Id(
            String name,
            Long cinemaItemId
    );

    // ================= CHECK DUPLICATE UPDATE =================
    boolean existsByNameIgnoreCaseAndCinemaItem_IdAndIdNot(
            String name,
            Long cinemaItemId,
            Long id
    );
}