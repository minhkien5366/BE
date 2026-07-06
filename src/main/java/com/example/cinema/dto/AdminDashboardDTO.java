// dto/AdminDashboardDTO.java
package com.example.cinema.dto;

public class AdminDashboardDTO {
    public double todayRevenue;
    public long todayTickets;
    public long todayShowtimes;
    public double occupancy;

    public AdminDashboardDTO(double todayRevenue, long todayTickets, long todayShowtimes, double occupancy) {
        this.todayRevenue = todayRevenue;
        this.todayTickets = todayTickets;
        this.todayShowtimes = todayShowtimes;
        this.occupancy = occupancy;
    }
}