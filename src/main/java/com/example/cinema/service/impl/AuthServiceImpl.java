package com.example.cinema.service.impl;

import com.example.cinema.dto.*;
import com.example.cinema.entity.*;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.*;
import com.example.cinema.security.JwtTokenProvider;
import com.example.cinema.service.AuthService;
import com.example.cinema.service.AuthMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthMailService authMailService;

    private static final String ROLE_USER = "ROLE_USER";

    // OTP CACHE
    private final Map<String, OtpDetails> otpCache = new ConcurrentHashMap<>();

    private static class OtpDetails {
        String otpCode;
        LocalDateTime expiryTime;

        OtpDetails(String otpCode, LocalDateTime expiryTime) {
            this.otpCode = otpCode;
            this.expiryTime = expiryTime;
        }
    }

    // ================= LOGIN =================
    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginRequest.getEmail().trim().toLowerCase(),
                                    loginRequest.getPassword().trim()
                            )
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = jwtTokenProvider.generateToken(authentication);

            List<String> roles = authentication.getAuthorities()
                    .stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            return JwtResponse.builder()
                    .token(token)
                    .email(loginRequest.getEmail())
                    .roles(roles)
                    .type("Bearer")
                    .build();

        } catch (BadCredentialsException e) {
            throw new RuntimeException("Email hoặc mật khẩu không chính xác!");
        } catch (Exception e) {
            throw new RuntimeException("Đăng nhập thất bại!");
        }
    }

    // ================= REGISTER =================
    @Override
    @Transactional
    public String register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new RuntimeException("Email không hợp lệ");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại!");
        }
        
        // 🎯 FIX MỚI: Kiểm tra xem số điện thoại đã tồn tại trong DB chưa
        String mobileNumber = request.getMobileNumber().trim();
        if (userRepository.existsByMobileNumber(mobileNumber)) {
            throw new RuntimeException("Số điện thoại này đã được đăng ký!");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new RuntimeException("Mật khẩu phải ít nhất 6 ký tự");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMobileNumber(mobileNumber);
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());

        // role
        Role userRole = roleRepository.findByRoleName(ROLE_USER)
                .orElseThrow(() -> new RuntimeException("ROLE_USER chưa tồn tại"));

        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        return "Đăng ký tài khoản thành công!";
    }
    
    // ================= FORGOT PASSWORD =================
    @Override
    public String forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }

        // Chuẩn hóa email
        String emailLower = email.trim().toLowerCase();

        if (!userRepository.existsByEmail(emailLower)) {
            throw new RuntimeException("Email không tồn tại!");
        }

        OtpDetails oldOtp = otpCache.get(emailLower);
        if (oldOtp != null && oldOtp.expiryTime.isAfter(LocalDateTime.now())) {
            throw new RuntimeException("OTP đã được gửi, vui lòng chờ 5 phút");
        }

        String otpCode = String.format("%06d", new Random().nextInt(999999));

        // Lưu email đã chuẩn hóa vào cache
        otpCache.put(emailLower,
                new OtpDetails(otpCode, LocalDateTime.now().plusMinutes(5))
        );

        authMailService.sendOtpEmail(emailLower, otpCode);

        return "OTP đã gửi về email (hiệu lực 5 phút)";
    }

    // ================= RESET PASSWORD =================
    @Override
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        // Chuẩn hóa email từ request
        String email = request.getEmail().trim().toLowerCase();
        OtpDetails otp = otpCache.get(email);

        if (otp == null) {
            throw new RuntimeException("OTP không hợp lệ hoặc đã hết hạn");
        }

        if (LocalDateTime.now().isAfter(otp.expiryTime)) {
            otpCache.remove(email);
            throw new RuntimeException("OTP đã hết hạn");
        }

        if (!otp.otpCode.equals(request.getOtpCode().trim())) {
            throw new RuntimeException("OTP không đúng");
        }

        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("Mật khẩu phải ít nhất 6 ký tự");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xóa sạch cache sau khi đổi mật khẩu thành công
        otpCache.remove(email);

        return "Đặt lại mật khẩu thành công!";
    }
}