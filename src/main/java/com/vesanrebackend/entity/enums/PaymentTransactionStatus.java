package com.vesanrebackend.entity.enums;

/**
 * Trạng thái của giao dịch thanh toán.
 */
public enum PaymentTransactionStatus {
    // Đang chờ xử lý.
    PENDING,
    // Đã thành công.
    SUCCEEDED,
    // Xử lý thất bại.
    FAILED
}
