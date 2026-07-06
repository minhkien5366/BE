package com.example.cinema.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats")
@Data
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;       
    private String seatRow;    
    private String seatNumber; 
    private String seatType;   
    private Double price;
    
    @Column(columnDefinition = "varchar(20) default 'AVAILABLE'")
    private String status = "AVAILABLE"; 

    @ManyToOne
    @JoinColumn(name = "room_id")
    @JsonIgnoreProperties("seats") 
    private Room room;

    @Transient
    private String userAvatar;

    // 🔥 Dùng @JsonProperty trực tiếp trên getter để ép JSON phải bao gồm trường này
    @JsonProperty("userAvatar")
    public String getUserAvatar() {
        return userAvatar;
    }

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}