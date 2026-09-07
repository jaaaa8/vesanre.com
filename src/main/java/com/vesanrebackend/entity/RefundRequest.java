package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.RefundStatus;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Yêu cầu hoàn tiền cho giao dịch.
 */
@Entity
@Table(name = "refund_requests", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    // Đơn đặt sân liên quan.
    private Booking booking;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false, unique = true)
    // Giao dịch được hoàn tiền.
    private PaymentTransaction paymentTransaction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_webhook_event_id", unique = true)
    // Webhook kích hoạt hoàn tiền.
    private PaymentWebhookEvent triggerWebhookEvent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmation_webhook_event_id", unique = true)
    // Webhook xác nhận hoàn tiền.
    private PaymentWebhookEvent confirmationWebhookEvent;

    @Column(name = "provider_code", nullable = false, length = 50)
    // Mã nhà cung cấp thanh toán.
    private String providerCode;

    @Column(name = "idempotency_key", nullable = false, length = 120, unique = true)
    // Khóa chống xử lý hoàn tiền lặp.
    private String idempotencyKey;

    @Column(name = "provider_refund_id", length = 160)
    // Mã hoàn tiền tại nhà cung cấp.
    private String providerRefundId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    // Số tiền.
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    // Mã tiền tệ ISO 4217.
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    // Số lần đã thử xử lý.
    private Integer attemptCount = 0;

    @Column(name = "next_retry_at", columnDefinition = "timestamp with time zone")
    // Thời điểm thử hoàn tiền lại.
    private Instant nextRetryAt;

    @Column(name = "last_error", columnDefinition = "text")
    // Lỗi xử lý gần nhất.
    private String lastError;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    // Người dùng yêu cầu hoàn tiền.
    private UserAccount requestedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cập nhật gần nhất.
    private Instant updatedAt;

    @Column(name = "completed_at", columnDefinition = "timestamp with time zone")
    // Thời điểm hoàn tất.
    private Instant completedAt;
}
