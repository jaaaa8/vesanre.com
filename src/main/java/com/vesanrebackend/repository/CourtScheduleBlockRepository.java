package com.vesanrebackend.repository;

import com.vesanrebackend.entity.court.CourtScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CourtScheduleBlockRepository extends JpaRepository<CourtScheduleBlock, UUID> {
    // Half-open overlap with [from, to): a block that started the day before and runs past midnight counts.
    @Query("select b from CourtScheduleBlock b where b.court.id = :courtId "
            + "and b.state = com.vesanrebackend.entity.enums.ScheduleBlockState.ACTIVE "
            + "and b.startAt < :to and b.endAt > :from")
    List<CourtScheduleBlock> findActiveOverlapping(@Param("courtId") UUID courtId, @Param("from") Instant from,
                                                   @Param("to") Instant to);
}
