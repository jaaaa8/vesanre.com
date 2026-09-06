package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.WebhookEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_webhook_events", schema = "sporthub", uniqueConstraints = {
        @UniqueConstraint(name = "uq_webhook_events_provider_event", columnNames = {"provider_code", "provider_event_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "provider_code", nullable = false, length = 50)
    private String providerCode;

    @Column(name = "provider_event_id", nullable = false, length = 160)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WebhookEventStatus status = WebhookEventStatus.RECEIVED;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private Instant receivedAt;

    @Column(name = "processed_at", columnDefinition = "timestamp with time zone")
    private Instant processedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
