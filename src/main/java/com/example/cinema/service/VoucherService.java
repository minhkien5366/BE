package com.example.cinema.service;

import com.example.cinema.dto.PointsRewardRequest;
import com.example.cinema.dto.VoucherRequest;
import com.example.cinema.entity.Voucher;
import java.util.List;

public interface VoucherService {
    List<Voucher> getAllVouchers();
    
    Voucher createVoucher(VoucherRequest request);
    
    void deleteVoucher(Long id);

    Voucher updateVoucher(Long id, VoucherRequest request);

    List<Voucher> getAvailableVouchers(Long cinemaItemId);

    void saveVoucherToUser(Long userId, Long voucherId);

    List<Voucher> getVouchersByUser(Long userId);

    Voucher validateAndGetVoucher(String code, Long cinemaItemId, Double currentTotal);
    void redeemVoucher(Long voucherId);
    void rewardPointsToUser(PointsRewardRequest request);
    List<Voucher> getRedeemableVouchers();
}