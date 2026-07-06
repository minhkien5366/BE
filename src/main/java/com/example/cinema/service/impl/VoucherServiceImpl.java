package com.example.cinema.service.impl;

import com.example.cinema.dto.PointsRewardRequest;
import com.example.cinema.dto.VoucherRequest;
import com.example.cinema.entity.*;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.*;
import com.example.cinema.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final PromotionRepository promotionRepository;
    private final PointHistoryRepository pointHistoryRepository;
    @Override
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    @Override
    @Transactional
    public Voucher createVoucher(VoucherRequest request) {
        validateSuperAdmin(); 

        if (voucherRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new RuntimeException("Mã đã tồn tại!");
        }

        Voucher voucher = Voucher.builder()
                .code(request.getCode().toUpperCase())
                .title(request.getTitle())
                .description(request.getDescription())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .costPoints(request.getCostPoints())
                .voucherType(
                        request.getPromotionId() != null
                                ? "EVENT"
                                : "REDEEM"
                )
                .build();

        if (request.getPromotionId() != null) {
            Promotion promotion = promotionRepository.findById(request.getPromotionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện"));
            voucher.setPromotion(promotion);
        }
        return voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public Voucher updateVoucher(Long id, VoucherRequest request) {

        validateSuperAdmin();

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id)
                );

        String newCode = request.getCode().toUpperCase();
        String oldCode = voucher.getCode();

        if (!oldCode.equalsIgnoreCase(newCode)) {

            if (voucherRepository.existsByCode(newCode)) {
                throw new RuntimeException(
                        "Mã giảm giá '" + newCode + "' đã tồn tại trên hệ thống!"
                );
            }
        }

        voucher.setCode(newCode);
        voucher.setTitle(request.getTitle());
        voucher.setDescription(request.getDescription());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderAmount(request.getMinOrderAmount());
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setCostPoints(request.getCostPoints());

        voucher.setVoucherType(
                request.getPromotionId() != null ? "EVENT" : "REDEEM"
        );

        if (request.getPromotionId() != null) {
            Promotion promotion = promotionRepository.findById(request.getPromotionId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Không tìm thấy sự kiện với ID: " + request.getPromotionId()
                            )
                    );
            voucher.setPromotion(promotion);
        } else {
            voucher.setPromotion(null);
        }

        return voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public Voucher validateAndGetVoucher(String code, Long cinemaItemId, Double currentTotal) {

        Voucher v = voucherRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không hợp lệ!"));

        LocalDateTime now = LocalDateTime.now();

        if (v.getStartDate() != null && now.isBefore(v.getStartDate())) {
            throw new RuntimeException("Mã giảm giá chưa đến thời gian áp dụng!");
        }

        if (v.getEndDate() != null && now.isAfter(v.getEndDate())) {
            throw new RuntimeException("Mã giảm giá đã hết hạn sử dụng!");
        }

        if (v.getUsedCount() >= v.getUsageLimit()) {
            throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng!");
        }

        if (currentTotal < v.getMinOrderAmount()) {
            throw new RuntimeException(
                    "Đơn hàng chưa đủ điều kiện tối thiểu (" +
                    String.format("%,.0f", v.getMinOrderAmount()) + "đ)");
        }

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        boolean hasVoucher = user.getVouchers()
                .stream()
                .anyMatch(uv -> uv.getId().equals(v.getId()));

        if (!hasVoucher) {
            throw new RuntimeException("Bạn chưa lưu mã này trong kho!");
        }

        return v;
    }
    
    @Override
    @Transactional
    public void saveVoucherToUser(Long userId, Long voucherId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));

        if (voucher.getEndDate() != null && LocalDateTime.now().isAfter(voucher.getEndDate())) {
            throw new RuntimeException("Mã này đã hết hạn, không thể lưu!");
        }

        if (user.getVouchers().stream().anyMatch(v -> v.getId().equals(voucherId))) {
            throw new RuntimeException("Mã này đã có trong kho ưu đãi của bạn!");
        }

        user.getVouchers().add(voucher);
        userRepository.save(user);
    }


    @Override
    public List<Voucher> getVouchersByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        return user.getVouchers();
    }

    @Override
    public List<Voucher> getAvailableVouchers(Long promotionId) {
        return voucherRepository.findActiveVouchersByPromotionId(
        promotionId,
        LocalDateTime.now()
            ).stream()
            .filter(v -> "EVENT".equalsIgnoreCase(v.getVoucherType()))
            .toList();}

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        validateSuperAdmin();
        Voucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá để xóa"));
        voucherRepository.delete(v);
    }

    private void validateSuperAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Vui lòng đăng nhập để tiếp tục!"));

        boolean isSuper = user.getRoles().stream()
                .anyMatch(r -> r.getRoleName().equalsIgnoreCase("SUPER_ADMIN") 
                            || r.getRoleName().equalsIgnoreCase("ROLE_SUPER_ADMIN"));

        if (!isSuper) {
            throw new RuntimeException("Quyền truy cập bị từ chối: Yêu cầu quyền Super Admin!");
        }
    }
    
    @Transactional
    public void redeemVoucher(Long voucherId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        Voucher voucher = voucherRepository.findById(voucherId).orElseThrow(() -> new ResourceNotFoundException("Voucher không tồn tại"));
        if (!"REDEEM".equalsIgnoreCase(voucher.getVoucherType())) {
            throw new RuntimeException("Voucher này không hỗ trợ đổi điểm");
        }
        if (voucher.getCostPoints() == null || voucher.getCostPoints() <= 0) throw new RuntimeException("Voucher không thể đổi bằng điểm");
        if (user.getPoints() < voucher.getCostPoints()) throw new RuntimeException("Không đủ điểm");
        if (user.getVouchers().stream().anyMatch(v -> v.getId().equals(voucherId))) throw new RuntimeException("Đã đổi voucher này rồi");

        user.setPoints(user.getPoints() - voucher.getCostPoints());
        user.getVouchers().add(voucher);
        userRepository.save(user);

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount(-voucher.getCostPoints())
                .description("Đổi voucher: " + voucher.getTitle())
                .type("REDEEMED")
                .build();
        pointHistoryRepository.save(history);
    }

    @Override
    @Transactional
    public void rewardPointsToUser(PointsRewardRequest request) {
        validateSuperAdmin();
        if (request.getPoints() == null || request.getPoints() <= 0) throw new RuntimeException("Số điểm phải lớn hơn 0!");

        User recipient = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + request.getEmail()));

        recipient.setPoints((recipient.getPoints() != null ? recipient.getPoints() : 0) + request.getPoints());
        userRepository.save(recipient);

        PointHistory history = PointHistory.builder()
                .user(recipient)
                .amount(request.getPoints())
                .description("Được Admin tặng điểm")
                .type("EARNED")
                .build();
        pointHistoryRepository.save(history);
    }

    @Override
    public List<Voucher> getRedeemableVouchers() {
        return voucherRepository.findRedeemableVouchers(LocalDateTime.now());
    }
}