package com.vesanrebackend.entity.enums;

/**
 * Loại hành động kiểm duyệt.
 */
public enum ModerationActionType {
    // Phê duyệt.
    APPROVE,
    // Từ chối.
    REJECT,
    // Tạm đình chỉ.
    SUSPEND,
    // Kích hoạt lại.
    REACTIVATE
}
