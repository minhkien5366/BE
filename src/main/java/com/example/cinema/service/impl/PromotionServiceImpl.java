package com.example.cinema.service.impl;

import com.example.cinema.dto.PromotionRequest;
import com.example.cinema.entity.*;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.*;
import com.example.cinema.service.CloudinaryService;
import com.example.cinema.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final CinemaItemRepository cinemaItemRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    // ================= GET =================

    @Override
    public List<Promotion> getPromotionsForClient(Long cinemaItemId) {
        return promotionRepository
                .findByCinemaItem_IdOrCinemaItemIsNull(cinemaItemId);
    }

    @Override
    public List<Promotion> getAllPromotions() {
        User user = getCurrentUser();
        if (isSuperAdmin(user)) {
            return promotionRepository.findAll();
        }
        return promotionRepository.findByCinemaItem_Id(
                user.getManagedCinemaItemId()
        );
    }

    @Override
    public Promotion getPromotionById(Long id) {

        return promotionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sự kiện không tồn tại!"
                        ));
    }

    // ================= CREATE =================

    @Override
    @Transactional
    public Promotion createPromotion(
            PromotionRequest request,
            MultipartFile file
    ) {

        // ================= CHECK DUPLICATE =================

        promotionRepository.findByTitle(
                request.getTitle().trim()
        ).ifPresent(p -> {
            throw new RuntimeException(
                    "Tên khuyến mãi đã tồn tại!"
            );
        });

        // ================= VALIDATE IMAGE =================

        if (file == null || file.isEmpty()) {
            throw new RuntimeException(
                    "Ảnh khuyến mãi không được để trống!"
            );
        }
        validateImage(file);

        // ================= CREATE =================
        Promotion promotion = new Promotion();
        mapRequestToEntity(request, promotion);
        uploadImage(promotion, file);
        return promotionRepository.save(promotion);
    }

    // ================= UPDATE =================

    @Override
    @Transactional
    public Promotion updatePromotion(
            Long id,
            PromotionRequest request,
            MultipartFile file
    ) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy sự kiện"
                        ));

        // ================= CHECK DUPLICATE =================
        promotionRepository.findByTitle(
                request.getTitle().trim()
        ).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException(
                        "Tên khuyến mãi đã tồn tại!"
                );
            }
        });

        // ================= UPDATE DATA =================

        mapRequestToEntity(request, promotion);

        // ================= UPDATE IMAGE =================
        if (file != null && !file.isEmpty()) {
            validateImage(file);
            deleteOldImage(promotion);
            uploadImage(promotion, file);
        }
        return promotionRepository.save(promotion);
    }

    // ================= DELETE =================

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sự kiện không tồn tại"
                        ));
        deleteOldImage(promotion);
        promotionRepository.delete(promotion);
    }

    // ================= HELPERS =================
    private void mapRequestToEntity(
            PromotionRequest request,
            Promotion promotion
    ) {
        promotion.setTitle(
                request.getTitle().trim()
        );
        promotion.setContent(
                request.getContent().trim()
        );
        if (
                request.getMovieId() != null &&
                request.getMovieId() != 0
        ) {
            promotion.setMovie(
                    movieRepository.findById(
                            request.getMovieId()
                    ).orElse(null)
            );
        } else {
            promotion.setMovie(null);
        }
        if (
                request.getCinemaItemId() != null &&
                request.getCinemaItemId() != 0
        ) {
            promotion.setCinemaItem(
                    cinemaItemRepository.findById(
                            request.getCinemaItemId()
                    ).orElse(null)
            );
        } else {
            promotion.setCinemaItem(null);
        }
    }

    // ================= IMAGE VALIDATION =================

    private void validateImage(MultipartFile file) {

        String contentType = file.getContentType();
        if (
                contentType == null ||
                (
                        !contentType.equals("image/png") &&
                        !contentType.equals("image/jpeg") &&
                        !contentType.equals("image/jpg") &&
                        !contentType.equals("image/webp")
                )
        ) {
            throw new RuntimeException(
                    "Ảnh phải là PNG, JPG, JPEG hoặc WEBP!"
            );
        }
        long maxSize = 2 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException(
                    "Ảnh không được vượt quá 2MB!"
            );
        }
    }

    // ================= UPLOAD IMAGE =================
    private void uploadImage(
            Promotion promotion,
            MultipartFile file
    ) {
        try {
            String url =
                    cloudinaryService.uploadImage(
                            file,
                            "promotions"
                    );
            promotion.setThumbnail(url);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Upload ảnh thất bại!"
            );
        }
    }

    // ================= DELETE OLD IMAGE =================

    private void deleteOldImage(Promotion promotion) {
        if (promotion.getThumbnail() == null) {
            return;
        }
        try {
            if (
                    promotion.getThumbnail()
                            .contains("cloudinary")
            ) {
                cloudinaryService.deleteImage(
                        promotion.getThumbnail()
                );
            }
        } catch (IOException e) {
            System.err.println(
                    "Không thể xóa ảnh cloud"
            );
        }
    }

    // ================= AUTH =================
    private User getCurrentUser() {
        String email =
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User không tồn tại"
                        ));
    }

    private boolean isSuperAdmin(User user) {
        return user.getRoles().stream().anyMatch(r ->
                r.getRoleName()
                        .equalsIgnoreCase("SUPER_ADMIN")
                        ||
                r.getRoleName()
                        .equalsIgnoreCase("ROLE_SUPER_ADMIN")
        );
    }
}