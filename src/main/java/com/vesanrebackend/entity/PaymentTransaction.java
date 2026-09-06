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
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_intent_id", nullable = false)
    private PaymentIntent paymentIntent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_event_id", unique = true)
    private PaymentWebhookEvent webhookEvent;

    @Column(name = "provider_code", nullable = false, length = 50)
    private String providerCode;

    @Column(name = "provider_transaction_id", nullable = false, length = 160)
    private String providerTransactionId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @OneToOne(mappedBy = "paymentTransaction", fetch = FetchType.LAZY)
    private RefundRequest refundRequest;
}
