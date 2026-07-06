package com.example.cinema.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

    // API siêu nhẹ, không gọi vào Database, chỉ để trả lời "Tôi đang thức"
    @GetMapping("/ping")
    public ResponseEntity<String> keepAwake() {
        return ResponseEntity.ok("Backend is awake!");
    }
}