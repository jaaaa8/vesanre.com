package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.VenueStatus;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Địa điểm hoặc chi nhánh thuộc cửa hàng.
 */
@Entity
@Table(name = "venues", schema = "sporthub", uniqueConstraints = {
        @UniqueConstraint(name = "uq_venues_shop_slug", columnNames = {"shop_id", "slug"})
})
@Getter
@Setter
@NoArgsConstructor
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    // Cửa hàng liên quan.
    private Shop shop;

    @Column(name = "slug", nullable = false, length = 120)
    // Chuỗi định danh dùng trong URL.
    private String slug;

    @Column(name = "name", nullable = false, length = 160)
    // Tên hiển thị.
    private String name;

    @Column(name = "description", columnDefinition = "text")
    // Mô tả chi tiết.
    private String description;

    @Column(name = "address_line", nullable = false, length = 255)
    // Địa chỉ chi tiết.
    private String addressLine;

    @Column(name = "ward", length = 120)
    // Phường hoặc xã.
    private String ward;

    @Column(name = "district", nullable = false, length = 120)
    // Quận hoặc huyện.
    private String district;

    @Column(name = "city", nullable = false, length = 120)
    // Thành phố trực thuộc trung ương.
    private String city;

    @Column(name = "province", length = 120)
    // Tỉnh.
    private String province;

    @Column(name = "postal_code", length = 20)
    // Mã bưu chính.
    private String postalCode;

    @Column(name = "latitude", precision = 9, scale = 6)
    // Vĩ độ địa điểm.
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    // Kinh độ địa điểm.
    private BigDecimal longitude;

    @Column(name = "timezone", nullable = false, length = 64)
    // Múi giờ IANA của địa điểm.
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "phone", length = 32)
    // Số điện thoại liên hệ.
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    // Trạng thái hiện tại.
    private VenueStatus status = VenueStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cập nhật gần nhất.
    private Instant updatedAt;

    @OneToMany(mappedBy = "venue")
    // Danh sách sân liên quan.
    private List<Court> courts = new ArrayList<>();

    @OneToMany(mappedBy = "venue")
    // Danh sách tiện ích liên quan.
    private List<VenueAmenity> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "venue")
    // Danh sách hình ảnh liên quan.
    private List<VenueImage> images = new ArrayList<>();
}
