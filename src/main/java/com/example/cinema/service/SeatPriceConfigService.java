package com.example.cinema.service;

import com.example.cinema.dto.SeatPriceConfigRequest;
import com.example.cinema.entity.SeatPriceConfig;
import java.util.List;

public interface SeatPriceConfigService {
    List<SeatPriceConfig> getAllConfigs();
    SeatPriceConfig createConfig(SeatPriceConfigRequest request);
    SeatPriceConfig updateConfig(Long id, SeatPriceConfigRequest request);
    void deleteConfig(Long id);
}