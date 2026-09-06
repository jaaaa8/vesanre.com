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

@Entity
@Table(name = "court_amenities", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CourtAmenity {

    @EmbeddedId
    private CourtAmenityId id = new CourtAmenityId();

    @MapsId("courtId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @MapsId("amenityId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amenity_id", nullable = false)
    private Amenity amenity;

    @Column(name = "details", length = 255)
    private String details;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class CourtAmenityId implements Serializable {
        @Column(name = "court_id", nullable = false)
        private UUID courtId;

        @Column(name = "amenity_id", nullable = false)
        private UUID amenityId;
    }
}
