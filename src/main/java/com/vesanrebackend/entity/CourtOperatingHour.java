package com.vesanrebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Giờ hoạt động của sân theo ngày trong tuần.
 */
@Entity
@Table(name = "court_operating_hours", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CourtOperatingHour {

    @EmbeddedId
    // Mã định danh bản ghi.
    private CourtOperatingHourId id = new CourtOperatingHourId();

    @MapsId("courtId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    // Sân liên quan.
    private Court court;

    @Column(name = "opens_at")
    // Giờ mở cửa.
    private LocalTime opensAt;

    @Column(name = "closes_at")
    // Giờ đóng cửa.
    private LocalTime closesAt;

    @Column(name = "is_closed", nullable = false)
    // Cho biết sân đóng cửa trong ngày.
    private Boolean isClosed = Boolean.FALSE;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class CourtOperatingHourId implements Serializable {
        @Column(name = "court_id", nullable = false)
        // Mã sân trong khóa ghép.
        private UUID courtId;

        @Column(name = "weekday", nullable = false)
        // Ngày trong tuần theo quy ước hệ thống.
        private Short weekday;
    }
}
