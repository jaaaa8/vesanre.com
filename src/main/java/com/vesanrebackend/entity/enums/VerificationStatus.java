package com.vesanrebackend.entity.enums;

/**
 * Trạng thái xét duyệt hồ sơ xác minh.
 */
public enum VerificationStatus {
    // Đang chờ xử lý.
    PENDING,
    // Đã được phê duyệt.
    APPROVED,
    // Đã bị từ chối.
    REJECTED
}
