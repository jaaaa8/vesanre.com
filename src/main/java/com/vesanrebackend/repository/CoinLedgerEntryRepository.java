package com.vesanrebackend.repository;

import com.vesanrebackend.entity.coin.CoinLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CoinLedgerEntryRepository extends JpaRepository<CoinLedgerEntry, Long> {
    @Query(value = "select e from CoinLedgerEntry e left join fetch e.booking where e.wallet.id = :walletId",
            countQuery = "select count(e) from CoinLedgerEntry e where e.wallet.id = :walletId")
    Page<CoinLedgerEntry> findByWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    @Query("select coalesce(sum(e.amount), 0) from CoinLedgerEntry e where e.wallet.id = :walletId")
    long sumByWalletId(@Param("walletId") UUID walletId);
}
