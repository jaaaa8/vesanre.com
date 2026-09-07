package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.CourtStatus;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sân thể thao thuộc một địa điểm.
 */
@Entity
@Table(name = "courts", schema = "sporthub", uniqueConstraints = {
        @UniqueConstraint(name = "uq_courts_venue_code", columnNames = {"venue_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    // Địa điểm liên quan.
    private Venue venue;

    @Column(name = "code", nullable = false, length = 50)
    // Mã nghiệp vụ duy nhất.
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    // Tên hiển thị.
    private String name;

    @Column(name = "description", columnDefinition = "text")
    // Mô tả chi tiết.
    private String description;

    @Column(name = "capacity", nullable = false)
    // Sức chứa của sân.
    private Integer capacity;

    @Column(name = "booking_step_minutes", nullable = false)
    // Bước thời gian đặt sân, tính bằng phút.
    private Integer bookingStepMinutes;

    @Column(name = "min_booking_minutes", nullable = false)
    // Thời lượng đặt tối thiểu, tính bằng phút.
    private Integer minBookingMinutes;

    @Column(name = "max_booking_minutes", nullable = false)
    // Thời lượng đặt tối đa, tính bằng phút.
    private Integer maxBookingMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private CourtStatus status = CourtStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cập nhật gần nhất.
    private Instant updatedAt;

    @OneToMany(mappedBy = "court")
    // Danh sách môn thể thao của sân.
    private List<CourtSport> sports = new ArrayList<>();

    @OneToMany(mappedBy = "court")
    // Danh sách tiện ích liên quan.
    private List<CourtAmenity> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "court")
    // Danh sách hình ảnh liên quan.
    private List<CourtImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "court")
    // Danh sách giờ hoạt động.
    private List<CourtOperatingHour> operatingHours = new ArrayList<>();

    @OneToMany(mappedBy = "court")
    // Danh sách quy tắc giá.
    private List<CourtPricingRule> pricingRules = new ArrayList<>();

    @OneToMany(mappedBy = "court")
    // Danh sách khoảng lịch của sân.
    private List<CourtScheduleBlock> scheduleBlocks = new ArrayList<>();
}
