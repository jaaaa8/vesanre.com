package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.AmenityScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "amenities", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "allowed_scope", nullable = false, length = 10)
    private AmenityScope allowedScope;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @OneToMany(mappedBy = "amenity")
    private List<VenueAmenity> venues = new ArrayList<>();

    @OneToMany(mappedBy = "amenity")
    private List<CourtAmenity> courts = new ArrayList<>();
}
