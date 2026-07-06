package com.example.cinema.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComboReportResponse {
    private Long comboId;
    private String comboName;
    private Long totalQuantity;
    private Double totalRevenue;
}