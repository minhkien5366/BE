package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.RoomRequest;
import com.example.cinema.entity.Room;
import com.example.cinema.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // ================= GET ALL =================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Room>>> getAllRooms() {

        return ResponseEntity.ok(
                ApiResponse.<List<Room>>builder()
                        .status(200)
                        .message("Lấy danh sách phòng theo quyền hạn thành công")
                        .data(roomService.getAllRooms())
                        .build()
        );
    }

    // ================= GET BY ID =================
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Room>> getRoomById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.<Room>builder()
                        .status(200)
                        .message("Lấy thông tin phòng thành công")
                        .data(roomService.getRoomById(id))
                        .build()
        );
    }
    
    // ================= GET BY CINEMA ITEM =================
    @GetMapping("/cinema-item/{cinemaItemId}")
    public ResponseEntity<ApiResponse<List<Room>>> getRoomsByCinemaItem(
            @PathVariable Long cinemaItemId
    ) {

        return ResponseEntity.ok(
                ApiResponse.<List<Room>>builder()
                        .status(200)
                        .message("Lấy danh sách phòng của chi nhánh thành công")
                        .data(roomService.getRoomsByCinemaItem(cinemaItemId))
                        .build()
        );
    }

    // ================= CREATE =================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Room>> createRoom(
            @Valid @RequestBody RoomRequest request
    ) {

        Room created = roomService.createRoom(request);

        return ResponseEntity.ok(
                ApiResponse.<Room>builder()
                        .status(201)
                        .message("Tạo phòng thành công")
                        .data(created)
                        .build()
        );
    }

    // ================= UPDATE =================
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Room>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request
    ) {

        Room updated = roomService.updateRoom(id, request);

        return ResponseEntity.ok(
                ApiResponse.<Room>builder()
                        .status(200)
                        .message("Cập nhật phòng thành công")
                        .data(updated)
                        .build()
        );
    }

    // ================= DELETE =================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteRoom(
            @PathVariable Long id
    ) {

        roomService.deleteRoom(id);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(200)
                        .message("Xóa phòng thành công")
                        .data("OK")
                        .build()
        );
    }
    
}