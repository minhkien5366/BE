package com.example.cinema.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Double amount;
    private String status;
    private String qrContent;
    private Long orderId;
    private LocalDateTime createdAt;
}