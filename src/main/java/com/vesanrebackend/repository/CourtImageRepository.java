package com.vesanrebackend.repository;

import com.vesanrebackend.entity.court.CourtImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourtImageRepository extends JpaRepository<CourtImage, UUID> {
    List<CourtImage> findByCourtIdOrderBySortOrder(UUID courtId);

    Optional<CourtImage> findByIdAndCourtId(UUID id, UUID courtId);

    // Images of every court of a venue (venue DRAFT delete).
    @Query("select i.storageKey from CourtImage i where i.court.venue.id = :venueId")
    List<String> findStorageKeysByVenueId(@Param("venueId") UUID venueId);
}
