package com.vesanrebackend.entity.enums;

/**
 * Trạng thái vòng đời của đơn đặt sân.
 */
public enum BookingStatus {
    // Đang giữ chỗ.
    HELD,
    // Đã xác nhận.
    CONFIRMED,
    // Đã hủy.
    CANCELLED,
    // Đã hết hạn.
    EXPIRED,
    // Đã hoàn tất.
    COMPLETED
}
