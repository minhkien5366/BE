package com.example.cinema.service.impl;

import com.example.cinema.dto.CinemaItemRequest;
import com.example.cinema.entity.Cinema;
import com.example.cinema.entity.CinemaItem;
import com.example.cinema.entity.Room;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.CinemaItemRepository;
import com.example.cinema.repository.CinemaRepository;
import com.example.cinema.repository.RoomRepository;
import com.example.cinema.repository.ShowtimeRepository;
import com.example.cinema.service.CinemaItemService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CinemaItemServiceImpl implements CinemaItemService {

    private final CinemaItemRepository itemRepository;

    private final CinemaRepository cinemaRepository;

    private final ShowtimeRepository showtimeRepository;

    private final RoomRepository roomRepository;

    // ================= GET ALL =================
    @Override
    public List<CinemaItem> getAllItems() {

        return itemRepository.findAll();
    }

    // ================= GET BY CITY =================
    @Override
    public List<CinemaItem> getByCity(String city) {

        return itemRepository.findByCity(city);
    }

    // ================= GET BY CINEMA =================
    @Override
    public List<CinemaItem> getByCinema(Long cinemaId) {

        return itemRepository.findByCinemaId(cinemaId);
    }

    // ================= GET DETAIL =================
    @Override
    public CinemaItem getItemById(Long id) {

        return itemRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Chi nhánh rạp không tồn tại với id: " + id
                        ));
    }

    // ================= CREATE =================
    @Override
    @Transactional
    public CinemaItem createItem(CinemaItemRequest request) {

        Cinema cinema = cinemaRepository.findById(
                        request.getCinemaId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cụm rạp không tồn tại"
                        ));

        // ================= CHECK TRÙNG TÊN =================
        boolean exists =
                itemRepository.existsByNameIgnoreCaseAndCinema_Id(
                        request.getName().trim(),
                        request.getCinemaId()
                );

        if (exists) {

            throw new RuntimeException(
                    "Tên chi nhánh đã tồn tại trong cụm rạp này!"
            );
        }

        CinemaItem item = new CinemaItem();

        mapRequestToEntity(
                request,
                item,
                cinema
        );

        return itemRepository.save(item);
    }

    // ================= UPDATE =================
    @Override
    @Transactional
    public CinemaItem updateItem(
            Long id,
            CinemaItemRequest request
    ) {

        CinemaItem item = itemRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Chi nhánh không tồn tại"
                        ));

        Cinema cinema = cinemaRepository.findById(
                        request.getCinemaId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cụm rạp không tồn tại"
                        ));

        // ================= CHECK TRÙNG TÊN =================
        boolean exists =
                itemRepository
                        .existsByNameIgnoreCaseAndCinema_IdAndIdNot(
                                request.getName().trim(),
                                request.getCinemaId(),
                                id
                        );

        if (exists) {

            throw new RuntimeException(
                    "Tên chi nhánh đã tồn tại trong cụm rạp này!"
            );
        }

        mapRequestToEntity(
                request,
                item,
                cinema
        );

        return itemRepository.save(item);
    }

    // ================= DELETE =================
    @Override
    @Transactional
    public void deleteItem(Long id) {

        CinemaItem item = itemRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy chi nhánh để xóa"
                        ));

        // ================= CHECK SUẤT CHIẾU =================
        boolean hasShowtime =
                showtimeRepository
                        .existsByCinemaItemIdAndEndTimeAfter(
                                id,
                                LocalDateTime.now()
                        );

        // ================= KHÔNG CHO XOÁ =================
        if (hasShowtime) {

            throw new RuntimeException(
                    "Không thể xóa chi nhánh vì vẫn còn suất chiếu đang hoạt động!"
            );
        }

        // ================= LẤY TOÀN BỘ PHÒNG =================
        List<Room> rooms =
                roomRepository.findByCinemaItem_Id(id);

        // ================= XOÁ TOÀN BỘ PHÒNG =================
        if (!rooms.isEmpty()) {

            roomRepository.deleteAll(rooms);
        }

        // ================= XOÁ CHI NHÁNH =================
        itemRepository.delete(item);
    }

    // ================= MAP =================
    private void mapRequestToEntity(
            CinemaItemRequest request,
            CinemaItem item,
            Cinema cinema
    ) {

        item.setName(
                request.getName().trim()
        );

        item.setAddress(
                request.getAddress().trim()
        );

        item.setCity(
                request.getCity().trim()
        );

        item.setHoursPerRoom(
                request.getHoursPerRoom()
        );

        item.setCinema(cinema);
    }
}