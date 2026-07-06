package com.example.cinema.repository;

import com.example.cinema.entity.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Lấy đơn hàng của người dùng
    List<Order> findByUserEmail(String email, Sort sort);

    // Lấy đơn hàng theo chi nhánh (Dùng cho Admin chi nhánh)
    List<Order> findByCinemaItem_Id(Long cinemaItemId, Sort sort);

    // --- THÊM MỚI: Truy vấn ID Suất chiếu từ một Đơn hàng (Dùng để đồng bộ Ticket) ---
    @Query("SELECT DISTINCT t.showtime.id FROM Order o " +
           "JOIN o.orderDetails od " +
           "JOIN Ticket t ON t.seat.id = od.itemId AND t.user.userId = o.user.userId " +
           "WHERE o.id = :orderId " +
           "AND od.itemType = 'TICKET' " +
           "AND t.status != 'CANCELLED'")
    List<Long> findShowtimeIdByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT o FROM Order o WHERE o.status = 'PAID'")
    Stream<Order> streamAllPaidOrders();
    
    List<Order> findByCinemaItemIdAndCreatedAtBetweenAndStatus(
        Long cinemaItemId, 
        LocalDateTime start, 
        LocalDateTime end, 
        String status
    );

    @Query("SELECT o.cinemaItem.name, SUM(o.totalAmount) FROM Order o " +
           "WHERE o.createdAt BETWEEN :start AND :end AND o.status = 'PAID' " +
           "GROUP BY o.cinemaItem.name ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> getCinemaRanking(LocalDateTime start, LocalDateTime end);
    
    List<Order> findByCreatedAtBetweenAndStatus(
        LocalDateTime start,
        LocalDateTime end,
        String status
    );

    @Query("""
    SELECT COALESCE(SUM(o.totalAmount),0)
    FROM Order o
    WHERE o.status = 'PAID'
    AND o.createdAt >= :start
    """)
    Double getTodayRevenue(LocalDateTime start);
    
    @Query("""
    SELECT COUNT(o)
    FROM Order o
    WHERE o.status = 'PAID'
    AND o.createdAt >= :start
    """)
    Long countTodayTickets(LocalDateTime start);

    @Query("""
    SELECT DATE(o.createdAt), SUM(o.totalAmount)
    FROM Order o
    WHERE o.status = 'PAID'
    AND o.createdAt >= :start
    GROUP BY DATE(o.createdAt)
    ORDER BY DATE(o.createdAt)
    """)
    List<Object[]> revenue7Days(LocalDateTime start);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt BETWEEN :start AND :end AND o.status = 'PAID'")
    Double sumRevenue(LocalDateTime start, LocalDateTime end);
    
    Long countByCreatedAtBetweenAndStatus(
        LocalDateTime start,
        LocalDateTime end,
        String status
    );

    @Query("""
    SELECT DATE(o.createdAt), SUM(o.totalAmount)
    FROM Order o
    WHERE o.status = 'PAID'
    AND o.createdAt >= :start
    AND o.cinemaItem.id = :cinemaId
    GROUP BY DATE(o.createdAt)
    ORDER BY DATE(o.createdAt)
    """)
    List<Object[]> revenue7DaysByCinema(Long cinemaId, LocalDateTime start);
    
    @Query("""
    SELECT SUM(o.totalAmount)
    FROM Order o
    WHERE o.cinemaItem.id = :cinemaId
    AND o.createdAt >= :startOfDay
    AND o.status = 'PAID'
    """)
    Double getTodayRevenueByCinema(
            @Param("cinemaId") Long cinemaId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    @Query("""
    SELECT COUNT(o)
    FROM Order o
    WHERE o.cinemaItem.id = :cinemaId
    AND o.createdAt >= :startOfDay
    AND o.status = 'PAID'
    """)
    Long countTodayTicketsByCinema(
            @Param("cinemaId") Long cinemaId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    // =====================================================================
    // 🎯 FIX CHÍ MẠNG: HÀM QUÉT ĐƠN HÀNG QUÁ HẠN DÀNH CHO CON BOT
    // =====================================================================
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt <= :thresholdTime")
    List<Order> findByStatusAndCreatedAtBefore(@Param("status") String status, @Param("thresholdTime") LocalDateTime thresholdTime);
}