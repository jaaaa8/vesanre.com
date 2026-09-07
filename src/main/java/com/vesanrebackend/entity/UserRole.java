package com.vesanrebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
 * Liên kết vai trò được cấp cho người dùng.
 */
@Entity
@Table(name = "user_roles", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class UserRole {

    @EmbeddedId
    // Mã định danh bản ghi.
    private UserRoleId id = new UserRoleId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    // Người dùng liên quan.
    private UserAccount user;

    @MapsId("roleCode")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_code", nullable = false)
    // Vai trò được cấp.
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    // Người dùng cấp vai trò.
    private UserAccount grantedBy;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cấp vai trò.
    private Instant grantedAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserRoleId implements Serializable {
        @Column(name = "user_id", nullable = false)
        // Mã người dùng trong khóa ghép.
        private UUID userId;

        @Column(name = "role_code", nullable = false, length = 30)
        // Mã vai trò trong khóa ghép.
        private String roleCode;
    }
}
