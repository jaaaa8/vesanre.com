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
    BOOKING_PAYOUT
}
