package com.example.cinema.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cinema_items") // Nên để số nhiều cho chuẩn DB
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String city;

    private Integer hoursPerRoom;

    // Liên kết với chuỗi rạp lớn (ví dụ: CGV, Lotte)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cinema_id")
    @JsonIgnoreProperties("cinemaItems") // Chống lặp JSON khi truy vấn từ Cinema
    private Cinema cinema;

    // Danh sách các phòng chiếu thuộc chi nhánh này
    @OneToMany(mappedBy = "cinemaItem", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("cinemaItem") // Chống lặp JSON khi truy vấn từ Room
    private List<Room> rooms;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}