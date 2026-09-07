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
 * Liên kết tiện ích được cung cấp tại địa điểm.
 */
@Entity
@Table(name = "venue_amenities", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class VenueAmenity {

    @EmbeddedId
    // Mã định danh bản ghi.
    private VenueAmenityId id = new VenueAmenityId();

    @MapsId("venueId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    // Địa điểm liên quan.
    private Venue venue;

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
    public static class VenueAmenityId implements Serializable {
        @Column(name = "venue_id", nullable = false)
        // Mã địa điểm trong khóa ghép.
        private UUID venueId;

        @Column(name = "amenity_id", nullable = false)
        // Mã tiện ích trong khóa ghép.
        private UUID amenityId;
    }
}
