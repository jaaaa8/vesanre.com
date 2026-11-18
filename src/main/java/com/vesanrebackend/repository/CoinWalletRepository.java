package com.vesanrebackend.repository;

import com.vesanrebackend.entity.coin.CoinWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CoinWalletRepository extends JpaRepository<CoinWallet, UUID> {
    // Insert-if-absent: concurrent first uses cannot create two wallets (unique user/shop).
    @Modifying
    @Query(value = "INSERT INTO sporthub.coin_wallets (id, user_id) VALUES (gen_random_uuid(), :userId) "
            + "ON CONFLICT (user_id) DO NOTHING", nativeQuery = true)
    int insertForUser(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "INSERT INTO sporthub.coin_wallets (id, shop_id) VALUES (gen_random_uuid(), :shopId) "
            + "ON CONFLICT (shop_id) DO NOTHING", nativeQuery = true)
    int insertForShop(@Param("shopId") UUID shopId);

    // Plain selects without joins, like VenueRepository.findByIdForUpdate.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from CoinWallet w where w.user.id = :userId")
    Optional<CoinWallet> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from CoinWallet w where w.shop.id = :shopId")
    Optional<CoinWallet> findByShopIdForUpdate(@Param("shopId") UUID shopId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from CoinWallet w where w.id = :id")
    Optional<CoinWallet> findByIdForUpdate(@Param("id") UUID id);

    @Query("select w from CoinWallet w where w.user.id = :userId")
    Optional<CoinWallet> findByUserId(@Param("userId") UUID userId);

    @Query("select w from CoinWallet w where w.shop.id = :shopId")
    Optional<CoinWallet> findByShopId(@Param("shopId") UUID shopId);
}
