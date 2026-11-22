package com.vesanrebackend.entity.coin;

import com.vesanrebackend.entity.account.UserAccount;
import com.vesanrebackend.entity.shop.Shop;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Ví coin của một người dùng hoặc một cửa hàng (đúng một trong hai).
 */
@Entity
@Table(name = "coin_wallets", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CoinWallet {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    // Người dùng sở hữu ví.
    private UserAccount user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", unique = true)
    // Cửa hàng sở hữu ví.
    private Shop shop;

    @Column(name = "balance", nullable = false)
    // Số dư coin hiện tại.
    private long balance;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cập nhật gần nhất.
    private Instant updatedAt;
}
