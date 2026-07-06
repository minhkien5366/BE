package com.example.cinema.repository;

import com.example.cinema.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}