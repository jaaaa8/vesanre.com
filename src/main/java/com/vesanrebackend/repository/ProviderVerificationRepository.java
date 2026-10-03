package com.vesanrebackend.repository;

import com.vesanrebackend.entity.provider.ProviderVerification;
import com.vesanrebackend.entity.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderVerificationRepository extends JpaRepository<ProviderVerification, UUID> {
    // At most one PENDING row per provider (partial unique index).
    @Query("select v from ProviderVerification v where v.provider.userId = :userId and v.status = :status")
    Optional<ProviderVerification> findByProviderAndStatus(@Param("userId") UUID userId, @Param("status") VerificationStatus status);

    @Query("select v from ProviderVerification v where v.provider.userId in :userIds order by v.createdAt desc")
    List<ProviderVerification> findByProviderIdsNewestFirst(@Param("userIds") Collection<UUID> userIds);
}
