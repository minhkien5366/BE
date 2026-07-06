package com.example.cinema.service;

import com.example.cinema.dto.CinemaItemRequest;
import com.example.cinema.entity.CinemaItem;
import java.util.List;

public interface CinemaItemService {
    List<CinemaItem> getAllItems();
    List<CinemaItem> getByCity(String city);
    List<CinemaItem> getByCinema(Long cinemaId);
    
    // Hàm mới bổ sung
    CinemaItem getItemById(Long id);

    CinemaItem createItem(CinemaItemRequest request);
    CinemaItem updateItem(Long id, CinemaItemRequest request);
    void deleteItem(Long id);
}