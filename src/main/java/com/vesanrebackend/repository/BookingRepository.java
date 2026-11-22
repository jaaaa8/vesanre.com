package com.vesanrebackend.repository;

import com.vesanrebackend.entity.booking.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {
    // Plain select without joins, like VenueRepository.findByIdForUpdate.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

    // Boolean ownership checks: callers must not load the entity before locking it.
    boolean existsByIdAndCustomerId(UUID id, UUID customerId);

    @Query("select count(b) > 0 from Booking b where b.id = :id and b.scheduleBlock.court.venue.shop.owner.userId = :userId")
    boolean existsForShopOwner(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select b.id from Booking b where b.status = com.vesanrebackend.entity.enums.BookingStatus.CONFIRMED "
            + "and b.scheduleBlock.endAt <= :now order by b.scheduleBlock.endAt")
    List<UUID> findIdsToComplete(@Param("now") Instant now, Pageable limit);
}
