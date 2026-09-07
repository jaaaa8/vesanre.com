package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.ShopMemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Thành viên vận hành cửa hàng.
 */
@Entity
@Table(name = "shop_members", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class ShopMember {

    @EmbeddedId
    // Mã định danh bản ghi.
    private ShopMemberId id = new ShopMemberId();

    @MapsId("shopId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    // Cửa hàng liên quan.
    private Shop shop;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    // Người dùng liên quan.
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20)
    // Vai trò thành viên trong cửa hàng.
    private ShopMemberRole memberRole;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tham gia cửa hàng.
    private Instant joinedAt;

    @Column(name = "deactivated_at", columnDefinition = "timestamp with time zone")
    // Thời điểm thành viên bị vô hiệu hóa.
    private Instant deactivatedAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ShopMemberId implements Serializable {
        @Column(name = "shop_id", nullable = false)
        // Mã cửa hàng trong khóa ghép.
        private UUID shopId;

        @Column(name = "user_id", nullable = false)
        // Mã người dùng trong khóa ghép.
        private UUID userId;
    }
}
