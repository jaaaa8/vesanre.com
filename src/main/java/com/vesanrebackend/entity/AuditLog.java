package com.vesanrebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Nhật ký kiểm toán các thay đổi trong hệ thống.
 */
@Entity
@Table(name = "audit_logs", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    // Người dùng thực hiện hành động.
    private UserAccount actorUser;

    @Column(name = "action", nullable = false, length = 100)
    // Hành động đã thực hiện.
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    // Loại thực thể bị tác động.
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    // Mã bản ghi bị tác động.
    private UUID entityId;

    @Column(name = "before_data", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    // Dữ liệu trước thay đổi, dạng JSON.
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    // Dữ liệu sau thay đổi, dạng JSON.
    private String afterData;

    @Column(name = "request_id", length = 120)
    // Mã yêu cầu dùng để truy vết.
    private String requestId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;
}
