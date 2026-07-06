package com.example.cinema.repository;

import com.example.cinema.entity.SeatLock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatLockRepository extends CrudRepository<SeatLock, String> {
    
}