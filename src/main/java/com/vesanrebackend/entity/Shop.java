package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.ShopStatus;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cửa hàng kinh doanh dịch vụ sân thể thao.
 */
@Entity
@Table(name = "shops", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false, unique = true)
    // Chủ sở hữu cửa hàng.
    private ProviderProfile owner;

    @Column(name = "slug", nullable = false, length = 120, unique = true)
    // Chuỗi định danh dùng trong URL.
    private String slug;

    @Column(name = "name", nullable = false, length = 160)
    // Tên hiển thị.
    private String name;

    @Column(name = "description", columnDefinition = "text")
    // Mô tả chi tiết.
    private String description;

    @Column(name = "logo_storage_key", length = 500)
    // Khóa lưu trữ tệp logo.
    private String logoStorageKey;

    @Column(name = "default_cancellation_policy", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    // Chính sách hủy mặc định, dạng JSON.
    private String defaultCancellationPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    // Trạng thái hiện tại.
    private ShopStatus status = ShopStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cập nhật gần nhất.
    private Instant updatedAt;

    @OneToMany(mappedBy = "shop")
    // Danh sách thành viên cửa hàng.
    private List<ShopMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "shop")
    // Danh sách địa điểm liên quan.
    private List<Venue> venues = new ArrayList<>();

    @OneToMany(mappedBy = "shop")
    // Danh sách hồ sơ xác minh.
    private List<ProviderVerification> verifications = new ArrayList<>();
}
