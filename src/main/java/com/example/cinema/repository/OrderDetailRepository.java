package com.example.cinema.repository;

import com.example.cinema.entity.OrderDetail;
import com.example.cinema.dto.ComboReportResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    List<OrderDetail> findByOrderId(Long orderId);

    // 1. DÀNH CHO ADMIN: Lọc chính xác theo CinemaId
    @Query("""
        SELECT new com.example.cinema.dto.ComboReportResponse(
            od.itemId,
            od.itemName,
            SUM(od.quantity),
            SUM(od.quantity * od.price)
        )
        FROM OrderDetail od
        JOIN od.order o
        WHERE od.itemType = 'COMBO'
        AND o.status = 'PAID'
        AND o.cinemaItem.id = :cinemaId
        AND o.createdAt BETWEEN :start AND :end
        GROUP BY od.itemId, od.itemName
        ORDER BY SUM(od.quantity) DESC
    """)
    List<ComboReportResponse> getBestSellingCombos(
            @Param("cinemaId") Long cinemaId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 2. DÀNH CHO SUPER-ADMIN: Lấy tổng toàn bộ các rạp hệ thống (Bỏ o.cinemaItem.id)
    @Query("""
        SELECT new com.example.cinema.dto.ComboReportResponse(
            od.itemId,
            od.itemName,
            SUM(od.quantity),
            SUM(od.quantity * od.price)
        )
        FROM OrderDetail od
        JOIN od.order o
        WHERE od.itemType = 'COMBO'
        AND o.status = 'PAID'
        AND o.createdAt BETWEEN :start AND :end
        GROUP BY od.itemId, od.itemName
        ORDER BY SUM(od.quantity) DESC
    """)
    List<ComboReportResponse> getAllBestSellingCombos(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    
    List<OrderDetail> findByItemTypeAndItemId(String itemType, Long itemId);
}