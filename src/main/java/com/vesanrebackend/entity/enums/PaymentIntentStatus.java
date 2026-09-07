package com.vesanrebackend.entity.enums;

/**
 * Trạng thái của ý định thanh toán.
 */
public enum PaymentIntentStatus {
    // Đã khởi tạo.
    CREATED,
    // Đang chờ xử lý.
    PENDING,
    // Đã thành công.
    SUCCEEDED,
    // Xử lý thất bại.
    FAILED,
    // Đã hết hạn.
    EXPIRED
}
