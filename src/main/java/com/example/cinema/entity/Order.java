package com.example.cinema.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@ToString
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double totalAmount;
    private String status;        
    private String paymentMethod;  

    @ManyToOne(fetch = FetchType.LAZY) // Chuyển sang LAZY chống bốc bừa bãi
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"password", "roles", "managedCinemaItemId"})
    @ToString.Exclude // Loại bỏ khỏi ToString để chặn đệ quy với User
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_item_id")
    @ToString.Exclude
    private CinemaItem cinemaItem;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("order")
    @ToString.Exclude // 🎯 CHÍ MẠNG: Ngắt đệ quy vòng lặp chéo với OrderDetail
    private List<OrderDetail> orderDetails;

    @CreationTimestamp
    private LocalDateTime createdAt;
}