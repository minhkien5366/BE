package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.RoleAssignmentRequest;
import com.example.cinema.dto.UserResponse;
import com.example.cinema.dto.UserUpdateRequest;
import com.example.cinema.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Lấy profile cá nhân
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .status(200)
                .message("Lấy thông tin cá nhân thành công")
                .data(userService.getMyProfile())
                .build());
    }

    // Cập nhật profile cá nhân
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .status(200)
                .message("Cập nhật profile thành công")
                .data(userService.updateProfile(request))
                .build());
    }

    // [SUPER ADMIN]: Phân quyền và chỉ định rạp quản lý cho người dùng
    @PutMapping("/{userId}/assign-role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable Long userId,
            @RequestBody RoleAssignmentRequest request) {
        
        UserResponse response = userService.assignRoleAndCinema(userId, request);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .status(200)
                .message("Phân quyền người dùng thành công")
                .data(response)
                .build());
    }

    // [ADMIN & SUPER ADMIN]: Tìm kiếm và quản lý danh sách người dùng
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<UserResponse> users = userService.getAllUsers(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<UserResponse>>builder()
                .status(200)
                .message("Lấy danh sách người dùng thành công")
                .data(users)
                .build());
    }
}