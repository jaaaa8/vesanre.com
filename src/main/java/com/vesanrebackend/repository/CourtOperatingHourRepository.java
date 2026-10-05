package com.vesanrebackend.repository;

import com.vesanrebackend.entity.court.CourtOperatingHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CourtOperatingHourRepository extends JpaRepository<CourtOperatingHour, CourtOperatingHour.CourtOperatingHourId> {
    // Bulk delete executes immediately, so re-inserting the same keys afterwards is safe.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CourtOperatingHour x where x.court.id = :courtId")
    void deleteByCourtId(@Param("courtId") UUID courtId);
}
