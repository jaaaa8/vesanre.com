package com.vesanrebackend.entity.enums;

/**
 * Trạng thái xử lý webhook thanh toán.
 */
public enum WebhookEventStatus {
    // Đã tiếp nhận.
    RECEIVED,
    // Đã xử lý.
    PROCESSED,
    // Đã bỏ qua.
    IGNORED,
    // Xử lý thất bại.
    FAILED
}
