package com.vesanrebackend.repository;

import com.vesanrebackend.entity.court.CourtSport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CourtSportRepository extends JpaRepository<CourtSport, CourtSport.CourtSportId> {
    // Bulk delete executes immediately, so re-inserting the same keys afterwards is safe.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CourtSport x where x.court.id = :courtId")
    void deleteByCourtId(@Param("courtId") UUID courtId);
}
