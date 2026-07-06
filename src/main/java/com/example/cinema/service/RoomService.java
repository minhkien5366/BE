package com.example.cinema.service;

import com.example.cinema.dto.RoomRequest;
import com.example.cinema.entity.Room;
import java.util.List;

public interface RoomService {
    List<Room> getAllRooms();
    List<Room> getRoomsByCinemaItem(Long cinemaItemId);
    Room getRoomById(Long id);
    Room createRoom(RoomRequest request);
    Room updateRoom(Long id, RoomRequest request);
    void deleteRoom(Long id);
}