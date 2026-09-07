package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Sự kiện chờ phát hành theo mẫu transactional outbox.
 */
@Entity
@Table(name = "outbox_events", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @Column(name = "event_key", nullable = false, length = 160, unique = true)
    // Khóa duy nhất của sự kiện.
    private String eventKey;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    // Loại aggregate phát sinh sự kiện.
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    // Mã aggregate liên quan.
    private UUID aggregateId;

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
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    // Số lần đã thử xử lý.
    private Integer attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm thử phát hành lại.
    private Instant nextAttemptAt;

    @Column(name = "locked_by", length = 120)
    // Tiến trình đang giữ khóa xử lý.
    private String lockedBy;

    @Column(name = "locked_at", columnDefinition = "timestamp with time zone")
    // Thời điểm khóa sự kiện để xử lý.
    private Instant lockedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @Column(name = "published_at", columnDefinition = "timestamp with time zone")
    // Thời điểm phát hành sự kiện.
    private Instant publishedAt;
}
