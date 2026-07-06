package com.example.cinema.service;

import com.example.cinema.dto.RoleAssignmentRequest; // Nhớ import DTO mới này
import com.example.cinema.dto.UserResponse;
import com.example.cinema.dto.UserUpdateRequest;
import org.springframework.data.domain.Page;

public interface UserService {
    /**
     * Lấy thông tin tài khoản của chính người đang đăng nhập
     */
    UserResponse getMyProfile();

    /**
     * Cập nhật thông tin cá nhân (Họ tên, SĐT, Ngày sinh, Giới tính, Avatar)
     */
    UserResponse updateProfile(UserUpdateRequest request);

    /**
     * Tìm kiếm và lấy danh sách người dùng phân trang (Dành cho ADMIN/SUPER_ADMIN)
     * @param keyword: Từ khóa tìm kiếm theo tên, email hoặc số điện thoại
     */
    Page<UserResponse> getAllUsers(String keyword, int page, int size);

    /**
     * [NÂNG CAO]: Phân quyền người dùng và chỉ định rạp phim quản lý
     * Chỉ dành cho SUPER_ADMIN sử dụng
     * @param userId: ID của người dùng cần được phân quyền
     * @param request: Chứa danh sách Role và ID của rạp phim (CinemaItem)
     */
    UserResponse assignRoleAndCinema(Long userId, RoleAssignmentRequest request);
}