package com.example.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_histories")
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor
@Builder
public class PointHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer amount; // Số điểm biến động (dương: cộng, âm: trừ)
    
    private String description; // Ví dụ: "Tặng điểm sự kiện", "Đổi voucher [Tên voucher]"
    
    private String type; // "EARNED" (tích lũy) hoặc "REDEEMED" (đổi quà)
    
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}