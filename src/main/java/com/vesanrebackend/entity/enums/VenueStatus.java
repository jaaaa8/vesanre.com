package com.vesanrebackend.entity.enums;

/**
 * Trạng thái xét duyệt và hoạt động của địa điểm.
 */
public enum VenueStatus {
    // Bản nháp.
    DRAFT,
    // Đang chờ xét duyệt.
    PENDING_REVIEW,
    // Đang hoạt động.
    ACTIVE,
    // Đã bị từ chối.
    REJECTED,
    // Đã bị tạm đình chỉ.
    SUSPENDED
}
