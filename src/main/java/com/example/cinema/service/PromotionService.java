package com.example.cinema.service;

import com.example.cinema.dto.PromotionRequest;
import com.example.cinema.entity.Promotion;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface PromotionService {
    List<Promotion> getPromotionsForClient(Long cinemaItemId);
    List<Promotion> getAllPromotions();
    Promotion getPromotionById(Long id);
    Promotion createPromotion(PromotionRequest request, MultipartFile file);
    Promotion updatePromotion(Long id, PromotionRequest request, MultipartFile file);
    void deletePromotion(Long id);
}