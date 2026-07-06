package com.example.cinema.service.impl;

import com.example.cinema.dto.RoleAssignmentRequest;
import com.example.cinema.dto.UserResponse;
import com.example.cinema.dto.UserUpdateRequest;
import com.example.cinema.entity.Role;
import com.example.cinema.entity.User;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.CinemaItemRepository;
import com.example.cinema.repository.RoleRepository;
import com.example.cinema.repository.UserRepository;
import com.example.cinema.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CinemaItemRepository cinemaItemRepository;

    @Override
    public UserResponse getMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        return mapToResponse(user);
    }

    // =========================
    // UPDATE PROFILE (RÀNG BUỘC CHẶT)
    // =========================
    @Override
    @Transactional
    public UserResponse updateProfile(UserUpdateRequest request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // ====== CHECK CHẶT TAY (DOUBLE VALIDATION) ======
        if (!StringUtils.hasText(request.getFirstName())) {
            throw new RuntimeException("Họ không được để trống");
        }

        if (!StringUtils.hasText(request.getLastName())) {
            throw new RuntimeException("Tên không được để trống");
        }

        if (!request.getMobileNumber().matches("^(0|\\+84)[0-9]{9,10}$")) {
            throw new RuntimeException("Số điện thoại không hợp lệ");
        }

        if (request.getDateOfBirth() != null &&
                request.getDateOfBirth().isAfter(java.time.LocalDate.now())) {
            throw new RuntimeException("Ngày sinh không hợp lệ");
        }

        if (request.getGender() != null &&
                !request.getGender().matches("MALE|FEMALE|OTHER")) {
            throw new RuntimeException("Giới tính không hợp lệ");
        }

        // ====== UPDATE ======
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setMobileNumber(request.getMobileNumber().trim());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());

        return mapToResponse(userRepository.save(user));
    }

    // =========================
    // GET ALL USERS
    // =========================
    @Override
    public Page<UserResponse> getAllUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> userPage = userRepository.searchUsers(keyword, pageable);
        return userPage.map(this::mapToResponse);
    }

    // =========================
    // ASSIGN ROLE + CINEMA
    // =========================
    @Override
    @Transactional
    public UserResponse assignRoleAndCinema(Long userId, RoleAssignmentRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        // ===== ROLE CHECK =====
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            throw new RuntimeException("Danh sách role không được để trống");
        }

        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại: " + roleName)))
                .collect(Collectors.toSet());

        user.setRoles(roles);

        // ===== CINEMA CHECK =====
        if (request.getCinemaItemId() != null) {
            boolean exists = cinemaItemRepository.existsById(request.getCinemaItemId());

            if (!exists) {
                throw new ResourceNotFoundException("Cinema không tồn tại");
            }

            user.setManagedCinemaItemId(request.getCinemaItemId());
        } else {
            user.setManagedCinemaItemId(null);
        }

        return mapToResponse(userRepository.save(user));
    }

    // =========================
    // MAPPING
    // =========================
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mobileNumber(user.getMobileNumber())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .managedCinemaItemId(user.getManagedCinemaItemId())
                .roles(user.getRoles().stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toSet()))
                .points(user.getPoints())
                .build();
    }
}