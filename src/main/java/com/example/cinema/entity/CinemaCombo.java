package com.example.cinema.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "cinema_combos",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"cinema_item_id", "combo_id"}
                )
        }
)
@Data
public class CinemaCombo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_item_id")
    private CinemaItem cinemaItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    private Combo combo;

    // ⭐ Dùng primitive boolean theo đúng yêu cầu của ông
    @Column(nullable = false)
    private boolean active = true;

    // 🔥 BỔ SUNG CHÍ MẠNG: Số lượng bắp nước tồn kho tại chi nhánh này
    // Dùng kiểu 'Integer' (không dùng int) để khi SuperAdmin tạo mặc định sẽ là null
    private Integer stock;
}