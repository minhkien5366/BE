package com.example.cinema.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("SeatLock") // Lưu vào Redis với Prefix "SeatLock"
public class SeatLock {

    @Id
    private String id; // Khóa chính sẽ ghép: {showtimeId}_{seatId} (VD: 5_12)

    private String userId; // Email hoặc ID người đang giữ ghế

    @TimeToLive
    private Long expiration; // Đồng hồ đếm ngược (Giây) - Hết giờ tự động xóa
}