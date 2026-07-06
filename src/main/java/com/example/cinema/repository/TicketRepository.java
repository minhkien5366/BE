package com.example.cinema.repository;

import com.example.cinema.entity.Seat;
import com.example.cinema.entity.Showtime;
import com.example.cinema.entity.Ticket;
import com.example.cinema.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByBookingCode(String bookingCode);
    
    List<Ticket> findByUserUserId(Long userId);

    List<Ticket> findByUserEmailOrderByCreatedAtDesc(String email);

    List<Ticket> findByShowtimeId(Long showtimeId);

    List<Ticket> findByShowtime_CinemaItem_Id(Long cinemaItemId, Sort sort);

    boolean existsBySeatAndShowtime(Seat seat, Showtime showtime);

    boolean existsBySeatAndShowtimeAndStatusIn(Seat seat, Showtime showtime, List<String> statuses);

    Optional<Ticket> findBySeatAndShowtimeAndUser(Seat seat, Showtime showtime, User user);

    boolean existsBySeatId(Long seatId);

    boolean existsBySeat_Room_Id(Long roomId);
    
    List<Ticket> findByBookingCodeIgnoreCase(String bookingCode);

    // 🎯 THÊM MỚI TẠI ĐÂY: Hàm gác cổng kiểm tra trạng thái vé của suất chiếu
    boolean existsByShowtimeIdAndStatus(Long showtimeId, String status);

    // --- FIX LỖI 1: Ràng buộc đã xem xong phim ---
    boolean existsByUser_UserIdAndShowtime_Movie_IdAndStatusAndShowtime_EndTimeBefore(
        Long userId, Long movieId, String status, LocalDateTime now);

    // --- FIX LỖI 2: Ràng buộc đã thanh toán vé (Dù phim chưa chiếu) ---
    boolean existsByUser_UserIdAndShowtime_Movie_IdAndStatus(Long userId, Long movieId, String status);

    // 🔥 UPDATE ĐẮT GIÁ: Đổi từ Optional sang List để ôm trọn toàn bộ cuống vé kẹt của vị trí ghế đó
    List<Ticket> findBySeatIdAndShowtimeId(Long seatId, Long showtimeId);
    
    boolean existsBySeatAndShowtimeAndStatusNotIgnoreCase(Seat seat, Showtime showtime, String status);
    
    // 🎯 THÊM MỚI: Lấy danh sách Top phim bán chạy nhất dựa trên tổng số vé đã thanh toán
    @Query("SELECT m.id AS movieId, m.title AS title, m.posterUrl AS posterUrl, COUNT(t.id) AS totalTickets " +
           "FROM Ticket t " +
           "JOIN t.showtime s " +
           "JOIN s.movie m " +
           "WHERE t.status IN ('PAID', 'USED') " +
           "GROUP BY m.id, m.title, m.posterUrl " +
           "ORDER BY totalTickets DESC")
    List<com.example.cinema.dto.TopMovieTicketDTO> findTopMoviesByTicketSales(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT t.seat FROM Ticket t WHERE t.showtime.id = :showtimeId AND t.status IN ('BOOKED', 'PAID')")
    List<Seat> findOccupiedSeatsByShowtimeId(@Param("showtimeId") Long showtimeId);

    // =========================================================================
    // 🎯 HÀM FIX LỖI MỚI NHẤT: BẮT SỐNG VÉ ĐỂ ĐỔI TRẠNG THÁI (TRÁNH LỖI KẸT BOOKED)
    // =========================================================================
    @Query("SELECT t FROM Ticket t WHERE t.seat.id = :seatId AND t.user.userId = :userId AND t.status = :status")
    List<Ticket> findBySeatIdAndUserIdAndStatus(@Param("seatId") Long seatId, @Param("userId") Long userId, @Param("status") String status);

    @Query(value = """
        SELECT
            m.title AS movieName,
            SUM(t.price) AS revenue
        FROM tickets t
        JOIN showtimes s ON t.showtime_id = s.id
        JOIN movies m ON s.movie_id = m.id
        WHERE t.status = 'PAID'
        AND t.created_at BETWEEN :startDate AND :endDate
        GROUP BY m.title
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<Object[]> getMovieRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

            
}