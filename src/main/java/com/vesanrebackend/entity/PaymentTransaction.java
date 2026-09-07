package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.PaymentTransactionStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Giao dịch phát sinh từ một ý định thanh toán.
 */
@Entity
@Table(name = "payment_transactions", schema = "sporthub", uniqueConstraints = {
        @UniqueConstraint(name = "uq_payment_transactions_provider_tx", columnNames = {"provider_code", "provider_transaction_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_intent_id", nullable = false)
    // Ý định thanh toán liên quan.
    private PaymentIntent paymentIntent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_event_id", unique = true)
    // Webhook thanh toán liên quan.
    private PaymentWebhookEvent webhookEvent;

    @Column(name = "provider_code", nullable = false, length = 50)
    // Mã nhà cung cấp thanh toán.
    private String providerCode;

    @Column(name = "provider_transaction_id", nullable = false, length = 160)
    // Mã giao dịch tại nhà cung cấp.
    private String providerTransactionId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    // Số tiền.
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    // Mã tiền tệ ISO 4217.
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm giao dịch xảy ra.
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @OneToOne(mappedBy = "paymentTransaction", fetch = FetchType.LAZY)
    // Yêu cầu hoàn tiền liên quan.
    private RefundRequest refundRequest;
}
