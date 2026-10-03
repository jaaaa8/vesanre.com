package com.vesanrebackend.repository;

import com.vesanrebackend.entity.court.CourtPricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CourtPricingRuleRepository extends JpaRepository<CourtPricingRule, UUID> {
    // Bulk delete executes immediately, so re-inserting the same keys afterwards is safe.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CourtPricingRule x where x.court.id = :courtId")
    void deleteByCourtId(@Param("courtId") UUID courtId);
}
