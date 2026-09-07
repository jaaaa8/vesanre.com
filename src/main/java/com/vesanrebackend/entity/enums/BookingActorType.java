package com.vesanrebackend.entity.enums;

/**
 * Loại chủ thể tác động đến trạng thái đơn đặt sân.
 */
public enum BookingActorType {
    // Khách hàng.
    CUSTOMER,
    // Nhà cung cấp.
    PROVIDER,
    // Quản trị viên hệ thống.
    ADMIN,
    // Hệ thống.
    SYSTEM,
    // Hệ thống thanh toán.
    PAYMENT
}
