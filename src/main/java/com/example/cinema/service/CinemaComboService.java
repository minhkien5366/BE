package com.example.cinema.service;

import com.example.cinema.dto.ComboAdminRequest;
import com.example.cinema.dto.ComboAdminResponse;
import com.example.cinema.dto.ComboClientResponse;

import java.util.List;

public interface CinemaComboService {

    List<ComboAdminResponse> getCombosForAdmin();

    Boolean toggleCombo(Long comboId);

    void updateComboStock(Long comboId, ComboAdminRequest request);

    List<ComboClientResponse> getActiveCombosForCinema(Long cinemaItemId);
}