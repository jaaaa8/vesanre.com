package com.vesanrebackend.entity.enums;

/**
 * Trạng thái của nhà cung cấp.
 */
public enum ProviderStatus {
    // Đang chờ xử lý.
    PENDING,
    // Đã xác minh.
    VERIFIED,
    // Đã bị từ chối.
    REJECTED,
    // Đã bị tạm đình chỉ.
    SUSPENDED
}
