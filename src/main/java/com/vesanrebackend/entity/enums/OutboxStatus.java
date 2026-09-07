package com.vesanrebackend.entity.enums;

/**
 * Trạng thái phát hành sự kiện outbox.
 */
public enum OutboxStatus {
    // Đang chờ xử lý.
    PENDING,
    // Đang xử lý.
    PROCESSING,
    // Đang chờ thử lại.
    RETRY_PENDING,
    // Đã công bố.
    PUBLISHED,
    // Xử lý thất bại.
    FAILED
}
