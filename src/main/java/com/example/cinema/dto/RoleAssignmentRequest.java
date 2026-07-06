package com.example.cinema.dto;

import lombok.Data;
import java.util.Set;

@Data
public class RoleAssignmentRequest {
    private Set<String> roles; // VD: ["ADMIN"]
    private Long cinemaItemId; // ID của rạp muốn giao cho Admin này quản lý (có thể để null nếu là Super Admin)
}