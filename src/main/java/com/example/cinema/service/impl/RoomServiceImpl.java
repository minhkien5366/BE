// ================= RoomServiceImpl.java =================
package com.example.cinema.service.impl;

import com.example.cinema.dto.RoomRequest;
import com.example.cinema.entity.CinemaItem;
import com.example.cinema.entity.Room;
import com.example.cinema.entity.Showtime;
import com.example.cinema.entity.User;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.CinemaItemRepository;
import com.example.cinema.repository.RoomRepository;
import com.example.cinema.repository.ShowtimeRepository;
import com.example.cinema.repository.UserRepository;
import com.example.cinema.service.RoomService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl
        implements RoomService {

    private final RoomRepository roomRepository;

    private final CinemaItemRepository cinemaItemRepository;

    private final UserRepository userRepository;

    private final ShowtimeRepository showtimeRepository;

    // ================= GET ALL =================
    @Override
    public List<Room> getAllRooms() {

        User currentUser = getCurrentUser();

        boolean isSuperAdmin =
                currentUser.getRoles().stream()
                        .anyMatch(r ->
                                r.getRoleName()
                                        .equals("SUPER_ADMIN")
                        );

        // ================= SUPER ADMIN =================
        if (isSuperAdmin) {

            return roomRepository.findAll();
        }

        // ================= ADMIN THƯỜNG =================
        Long managedCinemaId =
                currentUser.getManagedCinemaItemId();

        if (managedCinemaId == null) {

            throw new RuntimeException(
                    "Tài khoản Admin chưa được gán chi nhánh!"
            );
        }

        return roomRepository.findByCinemaItem_Id(
                managedCinemaId
        );
    }

    // ================= GET BY CINEMA =================
    @Override
    public List<Room> getRoomsByCinemaItem(
            Long cinemaItemId
    ) {

        return roomRepository.findByCinemaItem_Id(
                cinemaItemId
        );
    }

    // ================= GET DETAIL =================
    @Override
    public Room getRoomById(Long id) {

        return roomRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy phòng với ID: " + id
                        ));
    }

    // ================= CREATE =================
    @Override
    @Transactional
    public Room createRoom(
            RoomRequest request
    ) {

        // ================= VALIDATE =================
        validateRequest(request);

        // ================= CHECK PERMISSION =================
        validateAdminPermission(
                request.getCinemaItemId()
        );

        // ================= CHECK CINEMA =================
        CinemaItem cinemaItem =
                cinemaItemRepository.findById(
                                request.getCinemaItemId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Chi nhánh rạp không tồn tại"
                                ));

        // ================= CHECK DUPLICATE =================
        boolean exists =
                roomRepository
                        .existsByNameIgnoreCaseAndCinemaItem_Id(
                                request.getName().trim(),
                                request.getCinemaItemId()
                        );

        if (exists) {

            throw new RuntimeException(
                    "Tên phòng đã tồn tại trong chi nhánh này!"
            );
        }

        // ================= CREATE ROOM =================
        Room room = new Room();

        room.setName(
                request.getName().trim()
        );

        room.setTotalSeats(
                request.getTotalSeats()
        );

        room.setCinemaItem(
                cinemaItem
        );

        return roomRepository.save(room);
    }

    // ================= UPDATE =================
    @Override
    @Transactional
    public Room updateRoom(
            Long id,
            RoomRequest request
    ) {

        // ================= VALIDATE =================
        validateRequest(request);

        // ================= CHECK PERMISSION =================
        validateAdminPermission(
                request.getCinemaItemId()
        );

        // ================= FIND ROOM =================
        Room room =
                roomRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Phòng không tồn tại"
                                ));

        // ================= FIND CINEMA =================
        CinemaItem cinemaItem =
                cinemaItemRepository.findById(
                                request.getCinemaItemId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Chi nhánh rạp không tồn tại"
                                ));

        // ================= CHECK DUPLICATE =================
        boolean exists =
                roomRepository
                        .existsByNameIgnoreCaseAndCinemaItem_IdAndIdNot(
                                request.getName().trim(),
                                request.getCinemaItemId(),
                                id
                        );

        if (exists) {

            throw new RuntimeException(
                    "Tên phòng đã tồn tại trong chi nhánh này!"
            );
        }

        // ================= UPDATE =================
        room.setName(
                request.getName().trim()
        );

        room.setTotalSeats(
                request.getTotalSeats()
        );

        room.setCinemaItem(
                cinemaItem
        );

        return roomRepository.save(room);
    }

    // ================= DELETE =================
    @Override
    @Transactional
    public void deleteRoom(Long id) {

        // ================= FIND ROOM =================
        Room room =
                roomRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy phòng"
                                ));

        // ================= CHECK PERMISSION =================
        validateAdminPermission(
                room.getCinemaItem().getId()
        );

        // ================= CHECK SHOWTIME =================
        boolean hasFutureShowtime =
                showtimeRepository
                        .existsByRoomIdAndEndTimeAfter(
                                id,
                                LocalDateTime.now()
                        );

        if (hasFutureShowtime) {

            throw new RuntimeException(
                    "Không thể xóa phòng vì vẫn còn suất chiếu chưa diễn ra!"
            );
        }

        // ================= DETACH SHOWTIME =================
        List<Showtime> showtimes =
                showtimeRepository.findByRoomId(id);

        for (Showtime showtime : showtimes) {

            showtime.setRoom(null);
        }

        showtimeRepository.saveAll(showtimes);

        // ================= DELETE ROOM =================
        roomRepository.delete(room);
    }

    // ================= VALIDATE =================
    private void validateRequest(
            RoomRequest request
    ) {

        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Tên phòng không được để trống!"
            );
        }

        if (request.getTotalSeats() == null ||
                request.getTotalSeats() <= 0) {

            throw new RuntimeException(
                    "Tổng số ghế phải lớn hơn 0!"
            );
        }

        if (request.getCinemaItemId() == null) {

            throw new RuntimeException(
                    "Chi nhánh rạp không hợp lệ!"
            );
        }
    }

    // ================= CURRENT USER =================
    private User getCurrentUser() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Phiên đăng nhập đã hết hạn"
                        ));
    }

    // ================= CHECK ADMIN PERMISSION =================
    private void validateAdminPermission(
            Long targetCinemaId
    ) {

        User user = getCurrentUser();

        boolean isSuperAdmin =
                user.getRoles().stream()
                        .anyMatch(r ->
                                r.getRoleName()
                                        .equals("SUPER_ADMIN")
                        );

        // ================= SUPER ADMIN =================
        if (isSuperAdmin) {

            return;
        }

        // ================= ADMIN NORMAL =================
        if (user.getManagedCinemaItemId() == null ||
                !user.getManagedCinemaItemId()
                        .equals(targetCinemaId)) {

            throw new RuntimeException(
                    "Bạn không có quyền thao tác trên chi nhánh này!"
            );
        }
    }
}