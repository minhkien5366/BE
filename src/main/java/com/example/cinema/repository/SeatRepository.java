package com.example.cinema.repository;

import com.example.cinema.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByRoomId(Long roomId); 
    @Modifying // Bắt buộc khi dùng query xóa/cập nhật
    @Query("DELETE FROM Seat s WHERE s.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
    List<Seat> findByRoom_CinemaItem_Id(Long cinemaItemId);
    long countByRoomId(Long roomId);
}
