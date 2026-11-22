package com.vesanrebackend.repository;

import com.vesanrebackend.entity.payment.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {
    // 0 rows = duplicate event; a concurrent duplicate waits for the first insert to commit.
    @Modifying
    @Query(value = "INSERT INTO sporthub.payment_webhook_events (id, provider_code, provider_event_id, event_type, payload, received_at) "
            + "VALUES (:id, :providerCode, :providerEventId, :eventType, CAST(:payload AS jsonb), :receivedAt) "
            + "ON CONFLICT (provider_code, provider_event_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("providerCode") String providerCode,
                       @Param("providerEventId") String providerEventId, @Param("eventType") String eventType,
                       @Param("payload") String payload, @Param("receivedAt") Instant receivedAt);
}
