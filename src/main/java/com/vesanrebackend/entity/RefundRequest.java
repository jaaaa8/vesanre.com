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

@Entity
@Table(name = "refund_requests", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false, unique = true)
    private PaymentTransaction paymentTransaction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_webhook_event_id", unique = true)
    private PaymentWebhookEvent triggerWebhookEvent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmation_webhook_event_id", unique = true)
    private PaymentWebhookEvent confirmationWebhookEvent;

    @Column(name = "provider_code", nullable = false, length = 50)
    private String providerCode;

    @Column(name = "idempotency_key", nullable = false, length = 120, unique = true)
    private String idempotencyKey;

    @Column(name = "provider_refund_id", length = 160)
    private String providerRefundId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "next_retry_at", columnDefinition = "timestamp with time zone")
    private Instant nextRetryAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private UserAccount requestedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant updatedAt;

    @Column(name = "completed_at", columnDefinition = "timestamp with time zone")
    private Instant completedAt;
}
