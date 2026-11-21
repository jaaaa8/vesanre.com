package com.vesanrebackend.entity.enums;

/**
 * Trạng thái lệnh rút coin của shop.
 */
public enum WithdrawalStatus {
    // Chờ admin chuyển khoản; coin đã bị trừ.
    PENDING,
    // Admin đã chuyển tiền.
    PAID,
    // Admin từ chối; coin đã hoàn vào ví shop.
    REJECTED
}
