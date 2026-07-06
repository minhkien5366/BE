package com.example.cinema.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private String title;
    private String description;
    
    private Double discountValue; 
    private Double minOrderAmount; 
    
    private Integer usageLimit;    
    private Integer usedCount = 0; 

    private LocalDateTime startDate;
    private LocalDateTime endDate;


    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;
    private Integer costPoints;
    private String voucherType;
}