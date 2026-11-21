package com.vesanrebackend.entity.enums;

/**
 * Loại bút toán trong sổ coin.
 */
public enum CoinEntryType {
    // Nạp coin bằng chuyển khoản (+).
    TOP_UP,
    // Trừ coin đặt cọc khi đặt sân (-).
    BOOKING_DEPOSIT,
    // Trả coin cọc cho khách khi hủy (+).
    BOOKING_DEPOSIT_RETURN,
    // Chuyển coin cọc vào ví shop khi booking hoàn tất (+).
    BOOKING_PAYOUT,
    // Trừ coin ví shop khi tạo lệnh rút (-).
    WITHDRAWAL,
    // Hoàn coin vào ví shop khi lệnh rút bị từ chối (+).
    WITHDRAWAL_RETURN
}
