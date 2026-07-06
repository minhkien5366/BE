package com.example.cinema.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double price;
    private String status;
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "seat_id",
            nullable = true,
            foreignKey = @ForeignKey(
                    foreignKeyDefinition =
                            "FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE SET NULL"
            )
    )
    @JsonIgnoreProperties({"room"})
    @ToString.Exclude // Ngắt chuỗi quét vòng lặp với thực thể Seat
    private Seat seat;

    private String seatRow;
    private String seatNumber;
    private String seatName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id")
    @JsonIgnoreProperties({"cinemaItem", "room"})
    @ToString.Exclude // Ngắt chuỗi quét với Showtime
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"password", "roles", "managedCinemaItemId"})
    @ToString.Exclude // Ngắt chuỗi quét với User mua vé
    private User user;

    @CreationTimestamp
    private LocalDateTime createdAt;
}