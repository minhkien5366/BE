package com.example.cinema.service;

import com.example.cinema.entity.Order;

public interface MailService {
    void sendOrderConfirmation(Order order);
    void sendShowtimeCancellationEmail(com.example.cinema.entity.User user, com.example.cinema.entity.Showtime showtime, int points, boolean isSystemAuto);
}