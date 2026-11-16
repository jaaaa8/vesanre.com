package com.vesanrebackend.entity.enums;

/**
 * Trạng thái lệnh nạp coin.
 */
public enum CoinTopUpStatus {
    // Chờ khách chuyển khoản.
    PENDING,
    // Đã nhận tiền, coin đã cộng vào ví.
    PAID,
    // Quá hạn mà chưa nhận tiền.
    EXPIRED
}
