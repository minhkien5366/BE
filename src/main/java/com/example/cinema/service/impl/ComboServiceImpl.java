package com.example.cinema.service.impl;

import com.example.cinema.dto.ComboRequest;
import com.example.cinema.entity.CinemaCombo;
import com.example.cinema.entity.CinemaItem;
import com.example.cinema.entity.Combo;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.CinemaComboRepository;
import com.example.cinema.repository.CinemaItemRepository;
import com.example.cinema.repository.ComboRepository;
import com.example.cinema.service.CloudinaryService;
import com.example.cinema.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComboServiceImpl implements ComboService {

    private final ComboRepository comboRepository;
    private final CloudinaryService cloudinaryService;
    private final CinemaItemRepository cinemaItemRepository;
    private final CinemaComboRepository cinemaComboRepository;

    @Override
    public List<Combo> getAllCombos() {
        return comboRepository.findAll();
    }

    @Override
    public Combo getComboById(Long id) {
        return comboRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Combo không tồn tại ID: " + id
                        ));
    }

    @Override
    @Transactional
    public Combo createCombo(
            ComboRequest request,
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException(
                    "Ảnh combo không được để trống!"
            );
        }

        validateImage(file);

        comboRepository.findByName(
                request.getName().trim()
        ).ifPresent(c -> {
            throw new RuntimeException(
                    "Tên combo đã tồn tại!"
            );
        });

        Combo combo = new Combo();

        mapRequest(request, combo);

        uploadImage(combo, file);

        Combo savedCombo =
                comboRepository.save(combo);

        List<CinemaItem> allCinemas =
                cinemaItemRepository.findAll();

        List<CinemaCombo> cinemaCombos =
                allCinemas.stream().map(cinema -> {
                    CinemaCombo cc = new CinemaCombo();
                    cc.setCinemaItem(cinema);
                    cc.setCombo(savedCombo);
                    cc.setActive(true);
                    cc.setStock(null);
                    return cc;
                }).collect(Collectors.toList());

        cinemaComboRepository.saveAll(cinemaCombos);

        return savedCombo;
    }

    @Override
    @Transactional
    public Combo updateCombo(
            Long id,
            ComboRequest request,
            MultipartFile file
    ) {

        Combo combo = getComboById(id);

        comboRepository.findByName(
                request.getName().trim()
        ).ifPresent(existing -> {

            if (!existing.getId().equals(id)) {

                throw new RuntimeException(
                        "Tên combo đã tồn tại!"
                );
            }
        });

        mapRequest(request, combo);

        if (file != null && !file.isEmpty()) {

            validateImage(file);

            deleteOldImage(combo);

            uploadImage(combo, file);
        }

        return comboRepository.save(combo);
    }

    @Override
    @Transactional
    public void deleteCombo(Long id) {

        Combo combo = getComboById(id);

        deleteOldImage(combo);

        List<CinemaCombo> linkedCombos =
                cinemaComboRepository.findAll()
                        .stream()
                        .filter(cc ->
                                cc.getCombo()
                                        .getId()
                                        .equals(id)
                        )
                        .collect(Collectors.toList());

        if (!linkedCombos.isEmpty()) {
            cinemaComboRepository.deleteAll(linkedCombos);
        }

        comboRepository.delete(combo);
    }

    private void mapRequest(
            ComboRequest request,
            Combo combo
    ) {

        combo.setName(
                request.getName().trim()
        );

        combo.setDescription(
                request.getDescription().trim()
        );

        combo.setPrice(
                request.getPrice()
        );
    }

    private void validateImage(
            MultipartFile file
    ) {

        String contentType =
                file.getContentType();

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

        long maxSize =
                2 * 1024 * 1024;

        if (file.getSize() > maxSize) {

            throw new RuntimeException(
                    "Ảnh không được vượt quá 2MB!"
            );
        }
    }

    private void uploadImage(
            Combo combo,
            MultipartFile file
    ) {

        try {

            String url =
                    cloudinaryService.uploadImage(
                            file,
                            "combos"
                    );

            combo.setImageUrl(url);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Upload ảnh thất bại!"
            );
        }
    }

    private void deleteOldImage(
            Combo combo
    ) {

        if (combo.getImageUrl() == null) {
            return;
        }

        try {

            cloudinaryService.deleteImage(
                    combo.getImageUrl()
            );

        } catch (IOException e) {

            System.err.println(
                    "Không xoá được ảnh cũ"
            );
        }
    }
}