package com.vesanrebackend.entity.coin;

import com.vesanrebackend.entity.booking.Booking;
import com.vesanrebackend.entity.enums.CoinEntryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Bút toán coin (chỉ ghi thêm); mỗi lần đổi số dư ví ghi đúng một dòng.
 */
@Entity
@Table(name = "coin_ledger_entries", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CoinLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    // Ví bị thay đổi số dư.
    private CoinWallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 30)
    // Loại bút toán.
    private CoinEntryType entryType;

    @Column(name = "amount", nullable = false)
    // Số coin thay đổi, âm khi trừ.
    private long amount;

    @Column(name = "balance_after", nullable = false)
    // Số dư ví sau bút toán.
    private long balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "top_up_id")
    // Lệnh nạp liên quan (TOP_UP).
    private CoinTopUp topUp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    // Đơn đặt sân liên quan (các loại BOOKING_*).
    private Booking booking;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;
}
