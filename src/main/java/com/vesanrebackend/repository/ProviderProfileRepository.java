package com.vesanrebackend.repository;

import com.vesanrebackend.entity.ProviderProfile;
import com.vesanrebackend.entity.enums.ProviderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, UUID> {
    // Plain select without joins: FOR UPDATE cannot lock the nullable side of an outer join.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProviderProfile p where p.userId = :userId")
    Optional<ProviderProfile> findByIdForUpdate(@Param("userId") UUID userId);

    @Query("select p from ProviderProfile p join fetch p.user left join fetch p.shop "
            + "where p.status = :status order by p.createdAt desc")
    List<ProviderProfile> findByStatusWithDetails(@Param("status") ProviderStatus status, Limit limit);
}
