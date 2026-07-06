package com.example.cinema.entity;

import jakarta.persistence.*; // Đảm bảo dùng jakarta thay vì javax
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity // <--- THIẾU DÒNG NÀY SẼ GÂY LỖI "Not a managed type"
@Table(name = "banners")
@Data
public class Banner {
    @Id // <--- BẮT BUỘC PHẢI CÓ
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String imageUrl;
    private String linkUrl;
    private String status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}