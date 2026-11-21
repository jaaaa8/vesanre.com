package com.vesanrebackend.entity.coin;

import com.vesanrebackend.entity.account.UserAccount;
import com.vesanrebackend.entity.enums.WithdrawalStatus;
import com.vesanrebackend.entity.shop.Shop;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lệnh rút coin của shop; coin bị trừ khi tạo, hoàn lại khi bị từ chối.
 */
@Entity
@Table(name = "shop_withdrawals", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class ShopWithdrawal {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    // Shop rút coin.
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    // Người tạo lệnh.
    private UserAccount requestedBy;

    @Column(name = "coin_amount", nullable = false)
    // Số coin rút (đã trừ khỏi ví).
    private long coinAmount;

    @Column(name = "fee_coins", nullable = false)
    // Phí rút tính bằng coin.
    private long feeCoins;

    @Column(name = "payout_vnd", nullable = false, precision = 14, scale = 2)
    // Số tiền thực chuyển = (coin_amount - fee_coins) * 1.000.
    private BigDecimal payoutVnd;

    @Column(name = "bank_name", nullable = false, length = 120)
    // Tên ngân hàng nhận.
    private String bankName;

    @Column(name = "bank_account_number", nullable = false, length = 40)
    // Số tài khoản nhận.
    private String bankAccountNumber;

    @Column(name = "bank_account_name", nullable = false, length = 160)
    // Tên chủ tài khoản nhận.
    private String bankAccountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "bank_transaction_ref", length = 120)
    // Mã giao dịch chuyển khoản (PAID).
    private String bankTransactionRef;

    @Column(name = "rejection_reason", columnDefinition = "text")
    // Lý do từ chối (REJECTED).
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    // Admin xử lý lệnh.
    private UserAccount reviewedBy;

    @Column(name = "reviewed_at", columnDefinition = "timestamp with time zone")
    // Thời điểm admin xử lý.
    private Instant reviewedAt;

    @Column(name = "paid_at", columnDefinition = "timestamp with time zone")
    // Thời điểm chuyển tiền.
    private Instant paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cập nhật gần nhất.
    private Instant updatedAt;
}
