package com.example.cinema.service.impl;

import com.example.cinema.dto.CinemaRequest;
import com.example.cinema.entity.Cinema;
import com.example.cinema.entity.CinemaItem;
import com.example.cinema.entity.Room;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.CinemaItemRepository;
import com.example.cinema.repository.CinemaRepository;
import com.example.cinema.repository.RoomRepository;
import com.example.cinema.repository.ShowtimeRepository;
import com.example.cinema.service.CinemaService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CinemaItemRepository cinemaItemRepository;
    private final RoomRepository roomRepository;
    private final ShowtimeRepository showtimeRepository;

    // ================= GET ALL =================
    @Override
    public List<Cinema> getAllCinemas() {
        return cinemaRepository.findAll();
    }

    // ================= GET BY ID =================
    @Override
    public Cinema getCinemaById(Long id) {

        return cinemaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cụm rạp không tồn tại với id: " + id
                        ));
    }

    // ================= CREATE =================
    @Override
    @Transactional
    public Cinema createCinema(CinemaRequest request) {

        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Tên cụm rạp không được để trống!"
            );
        }

        String cinemaName = request.getName().trim();

        // CHECK TRÙNG
        if (cinemaRepository.existsByNameIgnoreCase(cinemaName)) {

            throw new RuntimeException(
                    "Tên cụm rạp đã tồn tại!"
            );
        }

        Cinema cinema = new Cinema();
        cinema.setName(cinemaName);

        return cinemaRepository.save(cinema);
    }

    // ================= UPDATE =================
    @Override
    @Transactional
    public Cinema updateCinema(Long id, CinemaRequest request) {

        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cụm rạp không tồn tại để cập nhật"
                        ));

        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Tên cụm rạp không được để trống!"
            );
        }

        String cinemaName = request.getName().trim();

        // CHECK TRÙNG
        if (cinemaRepository.existsByNameIgnoreCaseAndIdNot(
                cinemaName,
                id
        )) {

            throw new RuntimeException(
                    "Tên cụm rạp đã tồn tại!"
            );
        }

        cinema.setName(cinemaName);

        return cinemaRepository.save(cinema);
    }

    // ================= DELETE =================
    @Override
    @Transactional
    public void deleteCinema(Long id) {

        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cụm rạp không tồn tại"
                        ));

        // ================= LẤY TOÀN BỘ RẠP CON =================
        List<CinemaItem> cinemaItems =
                cinemaItemRepository.findByCinemaId(id);

        // ================= CHECK SUẤT CHIẾU =================
        for (CinemaItem cinemaItem : cinemaItems) {

            boolean hasShowtime =
                    showtimeRepository
                            .existsByCinemaItemIdAndEndTimeAfter(
                                    cinemaItem.getId(),
                                    LocalDateTime.now()
                            );

            if (hasShowtime) {

                throw new RuntimeException(
                        "Không thể xóa cụm rạp vì rạp \"" +
                                cinemaItem.getName() +
                                "\" vẫn còn suất chiếu!"
                );
            }
        }

        // ================= XOÁ PHÒNG =================
        for (CinemaItem cinemaItem : cinemaItems) {

            List<Room> rooms =
                    roomRepository.findByCinemaItem_Id(
                            cinemaItem.getId()
                    );

            if (!rooms.isEmpty()) {
                roomRepository.deleteAll(rooms);
            }
        }

        // ================= XOÁ RẠP CON =================
        if (!cinemaItems.isEmpty()) {
            cinemaItemRepository.deleteAll(cinemaItems);
        }

        // ================= XOÁ CỤM RẠP =================
        cinemaRepository.delete(cinema);
    }
}