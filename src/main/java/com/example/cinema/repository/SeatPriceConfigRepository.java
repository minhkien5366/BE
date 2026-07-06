package com.example.cinema.repository;

import com.example.cinema.entity.SeatPriceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SeatPriceConfigRepository extends JpaRepository<SeatPriceConfig, Long> {
    // Tìm giá theo: Loại ghế + Thứ + Rạp
    Optional<SeatPriceConfig> findBySeatTypeAndDayOfWeek(
            String seatType,
            Integer dayOfWeek
    );
}