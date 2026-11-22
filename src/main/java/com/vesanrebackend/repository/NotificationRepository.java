package com.vesanrebackend.repository;

import com.vesanrebackend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    // Returns 1 when inserted, 0 when the dedupe key already exists (redelivered event).
    @Modifying
    @Query(value = "INSERT INTO sporthub.notifications (id, user_id, booking_id, notification_type, title, body, dedupe_key) "
            + "VALUES (gen_random_uuid(), :userId, :bookingId, :type, :title, :body, :dedupeKey) "
            + "ON CONFLICT (dedupe_key) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("bookingId") UUID bookingId, @Param("type") String type,
                       @Param("title") String title, @Param("body") String body, @Param("dedupeKey") String dedupeKey);

    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadAtIsNull(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
}
