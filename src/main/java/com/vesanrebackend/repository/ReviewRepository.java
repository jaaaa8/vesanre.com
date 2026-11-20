package com.vesanrebackend.repository;

import com.vesanrebackend.dto.review.RatingSummary;
import com.vesanrebackend.entity.booking.Review;
import com.vesanrebackend.entity.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);

    @Query("select r from Review r where r.booking.scheduleBlock.court.venue.id = :venueId and r.status = :status")
    Page<Review> findByVenueAndStatus(@Param("venueId") UUID venueId, @Param("status") ReviewStatus status,
                                      Pageable pageable);

    @Query("select r from Review r where r.booking.scheduleBlock.court.venue.shop.owner.userId = :ownerId")
    Page<Review> findByShopOwner(@Param("ownerId") UUID ownerUserId, Pageable pageable);

    @Query("select r from Review r where r.id = :id and r.booking.scheduleBlock.court.venue.shop.owner.userId = :ownerId")
    Optional<Review> findOwned(@Param("id") UUID id, @Param("ownerId") UUID ownerUserId);

    @Query("select count(r) > 0 from Review r where r.id = :id "
            + "and r.booking.scheduleBlock.court.venue.shop.owner.userId = :ownerId")
    boolean existsOwned(@Param("id") UUID id, @Param("ownerId") UUID ownerUserId);

    // Bulk update skips @UpdateTimestamp, so updatedAt is set explicitly; the null guard makes a reply one-shot.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Review r set r.providerReply = :reply, r.repliedBy.id = :by, r.repliedAt = :at, r.updatedAt = :at "
            + "where r.id = :id and r.providerReply is null")
    int reply(@Param("id") UUID id, @Param("reply") String reply, @Param("by") UUID repliedBy, @Param("at") Instant at);

    @Query("select new com.vesanrebackend.dto.review.RatingSummary(avg(r.rating), count(r)) from Review r "
            + "where r.booking.scheduleBlock.court.venue.id = :venueId "
            + "and r.status = com.vesanrebackend.entity.enums.ReviewStatus.PUBLISHED")
    RatingSummary ratingOf(@Param("venueId") UUID venueId);
}
