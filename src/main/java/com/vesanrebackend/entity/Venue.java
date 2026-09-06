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
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @Column(name = "ward", length = 120)
    private String ward;

    @Column(name = "district", nullable = false, length = 120)
    private String district;

    @Column(name = "city", nullable = false, length = 120)
    private String city;

    @Column(name = "province", length = 120)
    private String province;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "phone", length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VenueStatus status = VenueStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant updatedAt;

    @OneToMany(mappedBy = "venue")
    private List<Court> courts = new ArrayList<>();

    @OneToMany(mappedBy = "venue")
    private List<VenueAmenity> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "venue")
    private List<VenueImage> images = new ArrayList<>();
}
