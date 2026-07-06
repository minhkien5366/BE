package com.example.cinema.controller;

import com.example.cinema.dto.ApiResponse;
import com.example.cinema.dto.PointsRewardRequest;
import com.example.cinema.dto.VoucherRequest;
import com.example.cinema.entity.PointHistory;
import com.example.cinema.entity.User;
import com.example.cinema.entity.Voucher;
import com.example.cinema.repository.PointHistoryRepository;
import com.example.cinema.repository.UserRepository;
import com.example.cinema.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

        private final VoucherService voucherService;
        private final UserRepository userRepository;
        private final PointHistoryRepository pointHistoryRepository; 

        @GetMapping("/promotion/{promotionId}")
        public ResponseEntity<ApiResponse<List<Voucher>>> getByPromotion(@PathVariable Long promotionId) {
                return ResponseEntity.ok(ApiResponse.<List<Voucher>>builder()
                        .status(200)
                        .message("Lấy danh sách mã thành công")
                        .data(voucherService.getAvailableVouchers(promotionId)) 
                        .build());
        }

        @PostMapping("/save/{voucherId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<String>> saveVoucher(
                @PathVariable Long voucherId, 
                @AuthenticationPrincipal UserDetails userDetails) {
                
                User user = userRepository.findByEmail(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("Lỗi xác thực"));
                voucherService.saveVoucherToUser(user.getUserId(), voucherId);
                
                return ResponseEntity.ok(ApiResponse.<String>builder()
                        .status(200)
                        .message("Đã lưu mã vào kho thành công!")
                        .build());
        }

        @GetMapping("/my-vouchers")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<Voucher>>> getMyVouchers(@AuthenticationPrincipal UserDetails userDetails) {
                User user = userRepository.findByEmail(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("Lỗi xác thực"));

                return ResponseEntity.ok(ApiResponse.<List<Voucher>>builder()
                        .status(200)
                        .data(voucherService.getVouchersByUser(user.getUserId()))
                        .build());
        }

        @PostMapping("/redeem/{voucherId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<String>> redeem(@PathVariable Long voucherId) {
                voucherService.redeemVoucher(voucherId);
                return ResponseEntity.ok(ApiResponse.<String>builder()
                        .status(200)
                        .message("Đổi voucher thành công!")
                        .build());
        }

        @GetMapping("/point-history")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<PointHistory>>> getPointHistory(@AuthenticationPrincipal UserDetails userDetails) {
                User user = userRepository.findByEmail(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User không tồn tại"));
                return ResponseEntity.ok(ApiResponse.<List<PointHistory>>builder()
                        .status(200)
                        .data(pointHistoryRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId()))
                        .build());
        }

        @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<List<Voucher>>> getAll() {
                return ResponseEntity.ok(ApiResponse.<List<Voucher>>builder()
                        .status(200).data(voucherService.getAllVouchers()).build());
        }

        @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<Voucher>> create(
                @Valid @RequestBody VoucherRequest request) {

        return ResponseEntity.status(201).body(ApiResponse.<Voucher>builder()
                .status(201)
                .data(voucherService.createVoucher(request))
                .build());
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasRole('SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<Voucher>> update(
                @PathVariable Long id,
                @Valid @RequestBody VoucherRequest request) {

        return ResponseEntity.ok(ApiResponse.<Voucher>builder()
                .status(200)
                .data(voucherService.updateVoucher(id, request))
                .build());
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
                voucherService.deleteVoucher(id);
                return ResponseEntity.ok(ApiResponse.<String>builder().status(200).build());
        }

        @PostMapping("/reward-points")
        @PreAuthorize("hasRole('SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<String>> rewardPoints(@RequestBody PointsRewardRequest request) {
                voucherService.rewardPointsToUser(request);
                return ResponseEntity.ok(ApiResponse.<String>builder()
                        .status(200)
                        .message("Tặng điểm thành công!")
                        .build());
        }
        @GetMapping("/redeemable")
        public ResponseEntity<ApiResponse<List<Voucher>>> getRedeemableVouchers() {
        return ResponseEntity.ok(ApiResponse.<List<Voucher>>builder()
                .status(200)
                .message("Lấy danh sách mã đổi điểm thành công")
                .data(voucherService.getRedeemableVouchers())
                .build());
        }
}