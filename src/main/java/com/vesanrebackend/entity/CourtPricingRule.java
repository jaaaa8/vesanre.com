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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Quy tắc giá của sân theo khung giờ.
 */
@Entity
@Table(name = "court_pricing_rules", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CourtPricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    // Sân liên quan.
    private Court court;

    @Column(name = "weekday", nullable = false)
    // Ngày trong tuần theo quy ước hệ thống.
    private Short weekday;

    @Column(name = "start_minute", nullable = false)
    // Phút bắt đầu tính từ đầu ngày.
    private Integer startMinute;

    @Column(name = "end_minute", nullable = false)
    // Phút kết thúc tính từ đầu ngày.
    private Integer endMinute;

    @Column(name = "price_per_hour", nullable = false, precision = 12, scale = 2)
    // Đơn giá theo giờ.
    private BigDecimal pricePerHour;

    @Column(name = "currency", nullable = false, length = 3)
    // Mã tiền tệ ISO 4217.
    private String currency;

    @Column(name = "is_active", nullable = false)
    // Cho biết bản ghi đang hoạt động.
    private Boolean isActive = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cập nhật gần nhất.
    private Instant updatedAt;
}
