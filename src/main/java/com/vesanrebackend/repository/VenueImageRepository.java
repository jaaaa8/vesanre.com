package com.vesanrebackend.repository;

import com.vesanrebackend.entity.venue.VenueImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueImageRepository extends JpaRepository<VenueImage, UUID> {
    List<VenueImage> findByVenueIdOrderBySortOrder(UUID venueId);

    Optional<VenueImage> findByIdAndVenueId(UUID id, UUID venueId);

    // Keys only: no entities enter the persistence context before the cascade delete.
    @Query("select i.storageKey from VenueImage i where i.venue.id = :venueId")
    List<String> findStorageKeysByVenueId(@Param("venueId") UUID venueId);
}
