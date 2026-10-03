package com.vesanrebackend.repository;

import com.vesanrebackend.entity.court.Court;
import com.vesanrebackend.entity.enums.CourtStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourtRepository extends JpaRepository<Court, UUID> {
    List<Court> findByVenueIdOrderByCode(UUID venueId);

    // Ownership chain court -> venue -> shop -> owner: another provider's court is simply not found.
    @Query("select c from Court c where c.id = :id and c.venue.shop.owner.userId = :ownerUserId")
    Optional<Court> findOwned(@Param("id") UUID id, @Param("ownerUserId") UUID ownerUserId);

    boolean existsByVenueIdAndStatus(UUID venueId, CourtStatus status);

    // Court children (sports, hours, prices...) go with it via ON DELETE CASCADE.
    @Modifying(flushAutomatically = true)
    @Query("delete from Court c where c.venue.id = :venueId")
    void deleteByVenueId(@Param("venueId") UUID venueId);
}
