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
 * Liên kết môn thể thao được sân hỗ trợ.
 */
@Entity
@Table(name = "court_sports", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CourtSport {

    @EmbeddedId
    // Mã định danh bản ghi.
    private CourtSportId id = new CourtSportId();

    @MapsId("courtId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    // Sân liên quan.
    private Court court;

    @MapsId("sportId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sport_id", nullable = false)
    // Môn thể thao được liên kết.
    private Sport sport;

    @Column(name = "is_primary", nullable = false)
    // Cho biết đây là môn thể thao chính.
    private Boolean isPrimary = Boolean.FALSE;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class CourtSportId implements Serializable {
        @Column(name = "court_id", nullable = false)
        // Mã sân trong khóa ghép.
        private UUID courtId;

        @Column(name = "sport_id", nullable = false)
        // Mã môn thể thao trong khóa ghép.
        private UUID sportId;
    }
}
