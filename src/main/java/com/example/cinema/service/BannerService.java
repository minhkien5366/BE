package com.example.cinema.service;

import com.example.cinema.dto.BannerRequest;
import com.example.cinema.entity.Banner;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface BannerService {
    List<Banner> getAllBanners();
    List<Banner> getActiveBanners();
    Banner getBannerById(Long id);
    Banner createBanner(BannerRequest request, MultipartFile file); 
    Banner updateBanner(Long id, BannerRequest request, MultipartFile file); 
    void deleteBanner(Long id);
}