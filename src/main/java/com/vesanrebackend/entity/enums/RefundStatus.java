package com.vesanrebackend.entity.enums;

/**
 * Trạng thái xử lý hoàn tiền.
 */
public enum RefundStatus {
    // Đang chờ xử lý.
    PENDING,
    // Đang xử lý.
    PROCESSING,
    // Đang chờ thử lại.
    RETRY_PENDING,
    // Đã thành công.
    SUCCEEDED,
    // Xử lý thất bại.
    FAILED
}
