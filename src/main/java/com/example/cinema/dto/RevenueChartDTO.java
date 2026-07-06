// dto/RevenueChartDTO.java
package com.example.cinema.dto;

public class RevenueChartDTO {
    public String day;
    public double revenue;

    public RevenueChartDTO(String day, double revenue) {
        this.day = day;
        this.revenue = revenue;
    }
}