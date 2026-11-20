package com.vesanrebackend.repository;

import com.vesanrebackend.entity.booking.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);
}
