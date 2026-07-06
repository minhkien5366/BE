package com.example.cinema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat_price_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatPriceConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String seatType; // NORMAL, VIP, COUPLE
    private Integer dayOfWeek; // 2 = Thứ 2, ..., 8 = Chủ Nhật
    private Double price;
}