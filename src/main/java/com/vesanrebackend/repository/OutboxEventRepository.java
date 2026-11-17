package com.vesanrebackend.repository;

import com.vesanrebackend.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    // skip locked: concurrent workers never claim the same row.
    @Query(value = "select * from sporthub.outbox_events where status in ('PENDING','RETRY_PENDING') "
            + "and next_attempt_at <= :now order by next_attempt_at, id limit 1 for update skip locked", nativeQuery = true)
    Optional<OutboxEvent> claimNext(@Param("now") Instant now);
}
