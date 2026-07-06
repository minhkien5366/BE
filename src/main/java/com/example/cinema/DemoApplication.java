package com.example.cinema;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	@PostConstruct
    public void init() {
        // Ép toàn bộ ứng dụng Spring Boot chạy theo múi giờ GMT+7 của Việt Nam
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
