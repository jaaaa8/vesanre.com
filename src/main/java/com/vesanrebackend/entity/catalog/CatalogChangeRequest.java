package com.vesanrebackend.entity.catalog;

import com.vesanrebackend.entity.account.UserAccount;
import com.vesanrebackend.entity.enums.ChangeRequestStatus;
import com.vesanrebackend.entity.enums.ChangeRequestTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Yêu cầu đổi trường quan trọng của shop/venue, chờ quản trị viên duyệt.
 */
@Entity
@Table(name = "catalog_change_requests", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CatalogChangeRequest {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    // Loại đối tượng bị đổi.
    private ChangeRequestTargetType targetType;

    @Column(name = "target_id", nullable = false)
    // Shop/venue bị đổi (không có khóa ngoại).
    private UUID targetId;

    @Column(name = "proposed", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    // Các trường quan trọng đề xuất, dạng JSON.
    private String proposed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private ChangeRequestStatus status = ChangeRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    // Người dùng gửi yêu cầu.
    private UserAccount submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    // Quản trị viên xét duyệt.
    private UserAccount reviewedBy;

    @Column(name = "reviewed_at", columnDefinition = "timestamp with time zone")
    // Thời điểm hoàn tất xét duyệt.
    private Instant reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "text")
    // Lý do từ chối.
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
