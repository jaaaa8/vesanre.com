package com.vesanrebackend.repository;

import com.vesanrebackend.entity.coin.CoinTopUp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CoinTopUpRepository extends JpaRepository<CoinTopUp, UUID> {
    Optional<CoinTopUp> findByIdAndWalletUserId(UUID id, UUID userId);

    Optional<CoinTopUp> findByTransferCode(String transferCode);

    @Modifying
    @Query("UPDATE CoinTopUp t SET t.status = com.vesanrebackend.entity.enums.CoinTopUpStatus.EXPIRED, t.updatedAt = :now "
            + "WHERE t.status = com.vesanrebackend.entity.enums.CoinTopUpStatus.PENDING AND t.expiresAt <= :now")
    int expirePending(@Param("now") Instant now);

    // Plain select without joins, so the wallet stays an unloaded proxy.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from CoinTopUp t where t.transferCode = :code")
    Optional<CoinTopUp> findByTransferCodeForUpdate(@Param("code") String code);
}
