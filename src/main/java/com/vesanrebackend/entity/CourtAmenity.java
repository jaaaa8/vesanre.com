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
import java.util.UUID;

/**
 * Liên kết tiện ích được trang bị cho sân.
 */
@Entity
@Table(name = "court_amenities", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CourtAmenity {

    @EmbeddedId
    // Mã định danh bản ghi.
    private CourtAmenityId id = new CourtAmenityId();

    @MapsId("courtId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    // Sân liên quan.
    private Court court;

    @MapsId("amenityId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amenity_id", nullable = false)
    // Tiện ích được liên kết.
    private Amenity amenity;

    @Column(name = "details", length = 255)
    // Thông tin bổ sung.
    private String details;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class CourtAmenityId implements Serializable {
        @Column(name = "court_id", nullable = false)
        // Mã sân trong khóa ghép.
        private UUID courtId;

        @Column(name = "amenity_id", nullable = false)
        // Mã tiện ích trong khóa ghép.
        private UUID amenityId;
    }
}
