package com.vesanrebackend.dto.admin;

import com.vesanrebackend.entity.account.UserAccount;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserItem(UUID id, String email, String displayName, String phone, List<String> roles,
                            String status, Instant createdAt) {
    // Touches the LAZY userRoles: call inside a transaction.
    public static AdminUserItem of(UserAccount u) {
        return new AdminUserItem(u.getId(), u.getEmail(), u.getDisplayName(), u.getPhone(),
                u.getUserRoles().stream().map(r -> r.getId().getRoleCode()).sorted().toList(),
                u.getStatus().name(), u.getCreatedAt());
    }
}
