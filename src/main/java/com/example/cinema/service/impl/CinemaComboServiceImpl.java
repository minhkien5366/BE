package com.example.cinema.service.impl;

import com.example.cinema.dto.ComboAdminRequest;
import com.example.cinema.dto.ComboAdminResponse;
import com.example.cinema.dto.ComboClientResponse; // 🔥 Tiêm DTO mới của khách hàng vào đây
import com.example.cinema.entity.*;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.*;
import com.example.cinema.service.CinemaComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CinemaComboServiceImpl implements CinemaComboService {

    private final CinemaComboRepository cinemaComboRepository;
    private final ComboRepository comboRepository;
    private final CinemaItemRepository cinemaItemRepository;
    private final UserRepository userRepository;

    // ================= GET ADMIN COMBOS =================

    @Override
    public List<ComboAdminResponse> getCombosForAdmin() {

        User user = getCurrentUser();
        Long cinemaItemId = user.getManagedCinemaItemId();

        List<Combo> allCombos = comboRepository.findAll();

        List<CinemaCombo> configs =
                cinemaComboRepository.findByCinemaItemId(cinemaItemId);

        return allCombos.stream().map(combo -> {

            CinemaCombo config = configs.stream()
                    .filter(c -> c.getCombo().getId().equals(combo.getId()))
                    .findFirst()
                    .orElse(null);

            boolean available =
                    config == null || config.isActive();

            return ComboAdminResponse.builder()
                    .id(combo.getId())
                    .name(combo.getName())
                    .description(combo.getDescription())
                    .imageUrl(combo.getImageUrl())
                    .price(combo.getPrice())
                    .isAvailable(available)
                    .stock(config != null ? config.getStock() : null) // 🔥 ĐÃ THÊM: Trả stock về cho trang quản trị Admin xem luôn
                    .build();

        }).collect(Collectors.toList());
    }

    // ================= TOGGLE =================

    @Override
    @Transactional
    public Boolean toggleCombo(Long comboId) {

        User user = getCurrentUser();
        Long cinemaItemId = user.getManagedCinemaItemId();

        CinemaCombo cinemaCombo =
                cinemaComboRepository
                        .findByCinemaItemIdAndComboId(cinemaItemId, comboId)
                        .orElse(null);

        if (cinemaCombo == null) {

            CinemaItem cinemaItem =
                    cinemaItemRepository.findById(cinemaItemId)
                            .orElseThrow();

            Combo combo =
                    comboRepository.findById(comboId)
                            .orElseThrow();

            cinemaCombo = new CinemaCombo();
            cinemaCombo.setCinemaItem(cinemaItem);
            cinemaCombo.setCombo(combo);
            cinemaCombo.setActive(false);

        } else {
            cinemaCombo.setActive(!cinemaCombo.isActive());
        }

        cinemaComboRepository.save(cinemaCombo);

        return cinemaCombo.isActive(); 
    }

    // ================= UPDATE STOCK FOR ADMIN (HÀM MỚI TINH) =================

        @Override
        @Transactional
        public void updateComboStock(Long comboId, ComboAdminRequest request) {

        User user = getCurrentUser();
        Long cinemaItemId = user.getManagedCinemaItemId();

        if (cinemaItemId == null) {
                throw new RuntimeException(
                        "Tài khoản của bạn chưa được gán quyền quản lý chi nhánh nào!"
                );
        }

        CinemaCombo cinemaCombo = cinemaComboRepository
                .findByCinemaItemIdAndComboId(cinemaItemId, comboId)
                .orElse(null);

        if (cinemaCombo == null) {

                CinemaItem cinemaItem = cinemaItemRepository
                        .findById(cinemaItemId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Không tìm thấy chi nhánh rạp"));

                Combo combo = comboRepository
                        .findById(comboId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Combo không tồn tại"));

                cinemaCombo = new CinemaCombo();
                cinemaCombo.setCinemaItem(cinemaItem);
                cinemaCombo.setCombo(combo);
                cinemaCombo.setActive(true);
        }


        cinemaCombo.setStock(request.getStock());

        cinemaComboRepository.save(cinemaCombo);
        }
    // ================= CUSTOMER (CẬP NHẬT CHUẨN ĐÉT KÈM STOCK) =================

    @Override
    public List<ComboClientResponse> getActiveCombosForCinema(Long cinemaItemId) {
        List<Combo> allCombos = comboRepository.findAll();

        List<CinemaCombo> configs = cinemaComboRepository.findByCinemaItemId(cinemaItemId);

        return allCombos.stream()
                .map(combo -> {
                    CinemaCombo config = configs.stream()
                            .filter(c -> c.getCombo().getId().equals(combo.getId()))
                            .findFirst()
                            .orElse(null);

                    boolean isActive = config == null || config.isActive();
                    
                    Integer stock = config != null ? config.getStock() : null;

                    if (isActive) {
                        return ComboClientResponse.builder()
                                .id(combo.getId())
                                .name(combo.getName())
                                .description(combo.getDescription())
                                .imageUrl(combo.getImageUrl())
                                .price(combo.getPrice())
                                .stock(stock)
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull) 
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        String email =
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));
    }
}