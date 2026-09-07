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

/**
 * Sự kiện webhook từ nhà cung cấp thanh toán.
 */
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
    // Mã định danh bản ghi.
    private UUID id;

    @Column(name = "provider_code", nullable = false, length = 50)
    // Mã nhà cung cấp thanh toán.
    private String providerCode;

    @Column(name = "provider_event_id", nullable = false, length = 160)
    // Mã sự kiện tại nhà cung cấp.
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    // Loại sự kiện.
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    // Dữ liệu sự kiện, dạng JSON.
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private WebhookEventStatus status = WebhookEventStatus.RECEIVED;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm nhận webhook.
    private Instant receivedAt;

    @Column(name = "processed_at", columnDefinition = "timestamp with time zone")
    // Thời điểm xử lý xong.
    private Instant processedAt;

    @Column(name = "error_message", columnDefinition = "text")
    // Thông báo lỗi xử lý.
    private String errorMessage;
}
