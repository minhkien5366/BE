package com.example.cinema.service;

import com.example.cinema.dto.ComboRequest;
import com.example.cinema.entity.Combo;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ComboService {
    List<Combo> getAllCombos();
    Combo getComboById(Long id);
    Combo createCombo(ComboRequest request, MultipartFile file); // Cập nhật
    Combo updateCombo(Long id, ComboRequest request, MultipartFile file); // Cập nhật
    void deleteCombo(Long id);
}