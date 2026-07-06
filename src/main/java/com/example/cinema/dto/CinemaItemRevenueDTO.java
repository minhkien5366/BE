package com.example.cinema.dto;

public class CinemaItemRevenueDTO {

    private Long id;
    private String name;
    private Double revenue;

    public CinemaItemRevenueDTO(Long id, String name, Double revenue) {
        this.id = id;
        this.name = name;
        this.revenue = revenue;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Double getRevenue() { return revenue; }
}