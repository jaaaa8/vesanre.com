package com.vesanrebackend.repository;

import com.vesanrebackend.entity.catalog.CatalogChangeRequest;
import com.vesanrebackend.entity.enums.ChangeRequestStatus;
import com.vesanrebackend.entity.enums.ChangeRequestTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CatalogChangeRequestRepository extends JpaRepository<CatalogChangeRequest, UUID> {
    // At most one PENDING row per target (partial unique index).
    Optional<CatalogChangeRequest> findByTargetTypeAndTargetIdAndStatus(
            ChangeRequestTargetType targetType, UUID targetId, ChangeRequestStatus status);

    @Query("select r from CatalogChangeRequest r where r.id = :id and r.submittedBy.id = :userId")
    Optional<CatalogChangeRequest> findByIdAndSubmitter(@Param("id") UUID id, @Param("userId") UUID userId);
}
