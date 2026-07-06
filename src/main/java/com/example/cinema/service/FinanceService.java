package com.example.cinema.service;

import java.time.LocalDateTime;
import java.util.Map;

public interface FinanceService {
    Map<String, Object> getMonthlyFinance(String month, Double taxRatePercent);
}