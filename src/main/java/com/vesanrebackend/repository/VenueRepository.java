package com.vesanrebackend.repository;

import com.vesanrebackend.entity.venue.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
    // Ownership filter: another provider's venue is simply not found.
    @Query("select v from Venue v where v.id = :id and v.shop.owner.userId = :ownerUserId")
    Optional<Venue> findOwned(@Param("id") UUID id, @Param("ownerUserId") UUID ownerUserId);

    List<Venue> findByShopIdOrderByCreatedAtDesc(UUID shopId);
}
