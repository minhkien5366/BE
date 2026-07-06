package com.example.cinema.service.impl;

import com.example.cinema.dto.BannerRequest;
import com.example.cinema.entity.Banner;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.BannerRepository;
import com.example.cinema.service.BannerService;
import com.example.cinema.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final CloudinaryService cloudinaryService;

    // ================= GET =================

    @Override
    public List<Banner> getAllBanners() {
        return bannerRepository.findAll();
    }

    @Override
    public List<Banner> getActiveBanners() {
        return bannerRepository.findByStatus("ACTIVE");
    }

    @Override
    public Banner getBannerById(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy Banner ID: " + id));
    }

    // ================= CREATE =================

    @Override
    @Transactional
    public Banner createBanner(BannerRequest request, MultipartFile file) {
        bannerRepository.findByTitle(request.getTitle())
                .ifPresent(b -> {
                    throw new RuntimeException("Tiêu đề banner đã tồn tại!");
                });

        Banner banner = new Banner();
        mapRequestToEntity(request, banner);

        if (file != null && !file.isEmpty()) {
            try {
                String url = cloudinaryService.uploadImage(file, "banners");
                banner.setImageUrl(url);
            } catch (IOException e) {
                throw new RuntimeException("Upload banner lỗi: " + e.getMessage());
            }
        }

        return bannerRepository.save(banner);
    }  
    // ================= UPDATE =================

    @Override
    @Transactional
    public Banner updateBanner(Long id, BannerRequest request, MultipartFile file) {

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy Banner ID: " + id));

        if (!banner.getTitle().equalsIgnoreCase(request.getTitle())) {

            bannerRepository.findByTitle(request.getTitle())
                    .ifPresent(existing -> {
                        throw new RuntimeException("Tiêu đề banner đã tồn tại!");
                    });
        }

        mapRequestToEntity(request, banner);

        if (file != null && !file.isEmpty()) {
            try {

                if (banner.getImageUrl() != null &&
                        banner.getImageUrl().contains("cloudinary")) {

                    cloudinaryService.deleteImage(banner.getImageUrl());
                }

                String url = cloudinaryService.uploadImage(file, "banners");
                banner.setImageUrl(url);

            } catch (IOException e) {
                throw new RuntimeException("Update banner lỗi: " + e.getMessage());
            }
        }

        return bannerRepository.save(banner);
    }
    // ================= DELETE =================

    @Override
    @Transactional
    public void deleteBanner(Long id) {

        Banner banner = bannerRepository.findById(id)
        .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy Banner ID: " + id));

        try {
            if (banner.getImageUrl() != null &&
                    banner.getImageUrl().contains("cloudinary")) {

                cloudinaryService.deleteImage(banner.getImageUrl());
            }
        } catch (IOException e) {
            System.err.println("Không thể xóa banner cloud: " + e.getMessage());
        }

        bannerRepository.delete(banner);
    }

    // ================= HELPER =================

    private void mapRequestToEntity(BannerRequest request, Banner banner) {
        banner.setTitle(request.getTitle());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setStatus(request.getStatus());
    }
}