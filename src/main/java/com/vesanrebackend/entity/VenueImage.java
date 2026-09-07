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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Hình ảnh của địa điểm.
 */
@Entity
@Table(name = "venue_images", schema = "sporthub", uniqueConstraints = {
        @UniqueConstraint(name = "uq_venue_images_venue_sort", columnNames = {"venue_id", "sort_order"})
})
@Getter
@Setter
@NoArgsConstructor
public class VenueImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    // Địa điểm liên quan.
    private Venue venue;

    @Column(name = "storage_key", nullable = false, length = 500, unique = true)
    // Khóa lưu trữ tệp hình ảnh.
    private String storageKey;

    @Column(name = "alt_text", length = 255)
    // Văn bản thay thế của hình ảnh.
    private String altText;

    @Column(name = "sort_order", nullable = false)
    // Thứ tự hiển thị.
    private Integer sortOrder;

    @Column(name = "is_cover", nullable = false)
    // Cho biết đây là ảnh đại diện.
    private Boolean isCover = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;
}
