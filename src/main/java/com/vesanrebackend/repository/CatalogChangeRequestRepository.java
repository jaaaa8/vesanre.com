package com.vesanrebackend.repository;

import com.vesanrebackend.entity.catalog.CatalogChangeRequest;
import com.vesanrebackend.entity.enums.ChangeRequestStatus;
import com.vesanrebackend.entity.enums.ChangeRequestTargetType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CatalogChangeRequestRepository extends JpaRepository<CatalogChangeRequest, UUID> {
    // At most one PENDING row per target (partial unique index).
    Optional<CatalogChangeRequest> findByTargetTypeAndTargetIdAndStatus(
            ChangeRequestTargetType targetType, UUID targetId, ChangeRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CatalogChangeRequest r where r.id = :id")
    Optional<CatalogChangeRequest> findByIdForUpdate(@Param("id") UUID id);

    Page<CatalogChangeRequest> findByStatus(ChangeRequestStatus status, Pageable pageable);

    Page<CatalogChangeRequest> findByStatusAndTargetType(ChangeRequestStatus status, ChangeRequestTargetType targetType,
                                                         Pageable pageable);
}
