package com.vesanrebackend.entity.coin;

import com.vesanrebackend.entity.enums.CoinTopUpStatus;
import com.vesanrebackend.entity.payment.PaymentWebhookEvent;
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
 * Lệnh nạp coin bằng chuyển khoản qua mã QR.
 */
@Entity
@Table(name = "coin_top_ups", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CoinTopUp {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    // Ví nhận coin.
    private CoinWallet wallet;

    @Column(name = "transfer_code", nullable = false, length = 30, unique = true)
    // Nội dung chuyển khoản dùng để đối chiếu.
    private String transferCode;

    @Column(name = "coin_amount", nullable = false)
    // Số coin sẽ cộng khi nhận tiền.
    private long coinAmount;

    @Column(name = "amount_vnd", nullable = false, precision = 12, scale = 2)
    // Số tiền phải chuyển; bằng coin_amount * 1.000.
    private BigDecimal amountVnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private CoinTopUpStatus status = CoinTopUpStatus.PENDING;

    @Column(name = "provider_code", nullable = false, length = 50)
    // Mã nhà cung cấp thanh toán.
    private String providerCode;

    @Column(name = "provider_transaction_id", length = 160)
    // Mã giao dịch tại nhà cung cấp.
    private String providerTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_event_id")
    // Webhook xác nhận đã nhận tiền.
    private PaymentWebhookEvent webhookEvent;

    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Hạn chuyển khoản.
    private Instant expiresAt;

    @Column(name = "paid_at", columnDefinition = "timestamp with time zone")
    // Thời điểm nhận tiền.
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
