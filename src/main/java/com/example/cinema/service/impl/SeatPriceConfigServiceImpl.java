package com.example.cinema.service.impl;

import com.example.cinema.dto.SeatPriceConfigRequest;
import com.example.cinema.entity.SeatPriceConfig;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.SeatPriceConfigRepository;
import com.example.cinema.service.SeatPriceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatPriceConfigServiceImpl implements SeatPriceConfigService {

    private final SeatPriceConfigRepository repository;

    @Override
    public List<SeatPriceConfig> getAllConfigs() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public SeatPriceConfig createConfig(SeatPriceConfigRequest request) {

        // ✅ kiểm tra trùng cấu hình (GHẾ + NGÀY)
        repository.findBySeatTypeAndDayOfWeek(
                request.getSeatType().toUpperCase(),
                request.getDayOfWeek()
        ).ifPresent(c -> {
            throw new RuntimeException("Cấu hình giá cho loại ghế và ngày này đã tồn tại!");
        });

        SeatPriceConfig config = SeatPriceConfig.builder()
                .seatType(request.getSeatType().toUpperCase())
                .dayOfWeek(request.getDayOfWeek())
                .price(request.getPrice())
                .build();

        return repository.save(config);
    }

    @Override
    @Transactional
    public SeatPriceConfig updateConfig(Long id, SeatPriceConfigRequest request) {

        SeatPriceConfig config = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy cấu hình giá"
                        ));

        repository.findBySeatTypeAndDayOfWeek(
                request.getSeatType().toUpperCase(),
                request.getDayOfWeek()
        ).ifPresent(existing -> {

            if (!existing.getId().equals(id)) {
                throw new RuntimeException(
                        "Cấu hình giá đã tồn tại!"
                );
            }
        });

        config.setSeatType(request.getSeatType().toUpperCase());
        config.setDayOfWeek(request.getDayOfWeek());
        config.setPrice(request.getPrice());

        return repository.save(config);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy cấu hình để xoá");
        }
        repository.deleteById(id);
    }
}