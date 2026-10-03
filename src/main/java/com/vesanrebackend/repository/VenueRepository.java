package com.vesanrebackend.repository;

import com.vesanrebackend.entity.enums.VenueStatus;
import com.vesanrebackend.entity.venue.Venue;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // Plain select without joins, like ProviderProfileRepository.findByIdForUpdate.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venue v where v.id = :id")
    Optional<Venue> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = "select v from Venue v join fetch v.shop where v.status = :status",
            countQuery = "select count(v) from Venue v where v.status = :status")
    Page<Venue> findByStatusWithShop(@Param("status") VenueStatus status, Pageable pageable);
}
