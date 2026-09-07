package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.VerificationStatus;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Hồ sơ xác minh nhà cung cấp hoặc cửa hàng.
 */
@Entity
@Table(name = "provider_verifications", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class ProviderVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    // Cửa hàng liên quan.
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    // Người dùng gửi hồ sơ xác minh.
    private UserAccount submittedBy;

    @Column(name = "documents", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    // Tài liệu xác minh, dạng JSON.
    private String documents;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private VerificationStatus status = VerificationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    // Quản trị viên xét duyệt.
    private UserAccount reviewedBy;

    @Column(name = "reviewed_at", columnDefinition = "timestamp with time zone")
    // Thời điểm hoàn tất xét duyệt.
    private Instant reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "text")
    // Lý do từ chối xác minh.
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cập nhật gần nhất.
    private Instant updatedAt;
}
