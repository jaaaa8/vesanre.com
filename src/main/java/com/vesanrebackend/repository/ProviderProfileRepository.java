package com.vesanrebackend.repository;

import com.vesanrebackend.entity.provider.ProviderProfile;
import com.vesanrebackend.entity.enums.ProviderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, UUID> {
    // Plain select without joins: FOR UPDATE cannot lock the nullable side of an outer join.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProviderProfile p where p.userId = :userId")
    Optional<ProviderProfile> findByIdForUpdate(@Param("userId") UUID userId);

    // Explicit count: Spring Data cannot derive one from a fetch-join query.
    @Query(value = "select p from ProviderProfile p join fetch p.user left join fetch p.shop where p.status = :status",
            countQuery = "select count(p) from ProviderProfile p where p.status = :status")
    Page<ProviderProfile> findByStatusWithDetails(@Param("status") ProviderStatus status, Pageable pageable);
}
