package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tài khoản người dùng.
 */
@Entity
@Table(name = "users", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @Column(name = "email", nullable = false, length = 320)
    // Địa chỉ email gốc.
    private String email;

    @Column(name = "email_normalized", nullable = false, length = 320, unique = true)
    // Email chuẩn hóa để tra cứu và chống trùng.
    private String emailNormalized;

    @Column(name = "password_hash", nullable = false, length = 255)
    // Mật khẩu đã băm.
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    // Tên hiển thị của người dùng.
    private String displayName;

    @Column(name = "phone", length = 32)
    // Số điện thoại liên hệ.
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified_at", columnDefinition = "timestamp with time zone")
    // Thời điểm xác minh email.
    private Instant emailVerifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cập nhật gần nhất.
    private Instant updatedAt;

    @OneToMany(mappedBy = "user")
    // Danh sách vai trò người dùng.
    private List<UserRole> userRoles = new ArrayList<>();

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    // Hồ sơ nhà cung cấp.
    private ProviderProfile providerProfile;
}
