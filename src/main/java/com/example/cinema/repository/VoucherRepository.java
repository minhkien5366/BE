package com.example.cinema.repository;

import com.example.cinema.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * DÀNH CHO CLIENT: Lấy mã còn hạn, còn lượt dùng theo Sự kiện
     */
    @Query("SELECT v FROM Voucher v WHERE v.promotion.id = :promotionId " +
           "AND v.startDate <= :now AND v.endDate >= :now " +
           "AND v.usedCount < v.usageLimit")
    List<Voucher> findActiveVouchersByPromotionId(@Param("promotionId") Long promotionId, 
                                                 @Param("now") LocalDateTime now);

    /**
     * Lấy tất cả mã của 1 sự kiện
     */
    List<Voucher> findByPromotionId(Long promotionId);

    /**
     * FIX CHÍNH: Lấy voucher khả dụng (Bỏ kiểm tra cinemaItem)
     * Giờ đây tất cả Voucher đều là dùng chung toàn hệ thống
     */
    @Query("SELECT v FROM Voucher v WHERE v.startDate <= :now " +
           "AND v.endDate >= :now " +
           "AND v.usedCount < v.usageLimit")
    List<Voucher> findAvailableVouchers(@Param("now") LocalDateTime now);

@Query("""
    SELECT v
    FROM Voucher v
    WHERE v.voucherType = 'REDEEM'
    AND (v.startDate IS NULL OR v.startDate <= :now)
    AND (v.endDate IS NULL OR v.endDate >= :now)
    AND v.usedCount < v.usageLimit
""")
List<Voucher> findRedeemableVouchers(LocalDateTime now);
}