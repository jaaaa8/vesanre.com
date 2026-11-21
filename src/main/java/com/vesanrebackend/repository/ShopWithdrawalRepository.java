package com.vesanrebackend.repository;

import com.vesanrebackend.entity.coin.ShopWithdrawal;
import com.vesanrebackend.entity.enums.WithdrawalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ShopWithdrawalRepository extends JpaRepository<ShopWithdrawal, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from ShopWithdrawal w where w.id = :id")
    Optional<ShopWithdrawal> findByIdForUpdate(@Param("id") UUID id);

    Page<ShopWithdrawal> findByShopId(UUID shopId, Pageable pageable);

    @Query(value = "select w from ShopWithdrawal w join fetch w.shop where w.status = :status",
            countQuery = "select count(w) from ShopWithdrawal w where w.status = :status")
    Page<ShopWithdrawal> findByStatus(@Param("status") WithdrawalStatus status, Pageable pageable);
}
