package com.vesanrebackend.service.admin;

import com.vesanrebackend.dto.admin.AdminUserItem;
import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.entity.account.UserAccount;
import com.vesanrebackend.entity.account.UserRole;
import com.vesanrebackend.entity.enums.UserStatus;
import com.vesanrebackend.repository.UserAccountRepository;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminUserService {
    private static final String ENTITY_TYPE = "USER";

    private final UserAccountRepository users;
    private final AdminAudit audit;

    public AdminUserService(UserAccountRepository users, AdminAudit audit) {
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserItem> list(String q, String role, UserStatus status, Pageable pageable) {
        Specification<UserAccount> spec = Specification.unrestricted();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((r, query, cb) -> cb.or(
                    cb.like(cb.lower(r.get("email")), pattern, '\\'),
                    cb.like(cb.lower(r.get("displayName")), pattern, '\\')));
        }
        if (role != null && !role.isBlank()) {
            String code = role.trim().toUpperCase(Locale.ROOT);
            spec = spec.and((r, query, cb) -> {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<UserRole> ur = sub.from(UserRole.class);
                sub.select(cb.literal(1)).where(cb.equal(ur.get("user"), r),
                        cb.equal(ur.get("id").get("roleCode"), code));
                return cb.exists(sub);
            });
        }
        if (status != null) {
            spec = spec.and((r, query, cb) -> cb.equal(r.get("status"), status));
        }
        return PageResponse.of(users.findAll(spec, pageable).map(AdminUserItem::of));
    }

    // The row lock makes a second admin wait, then see the new status and get 409 (one audit row).
    @Transactional
    public AdminUserItem suspend(UUID adminId, UUID userId, String reason) {
        UserAccount user = lock(userId);
        if (userId.equals(adminId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot suspend yourself");
        }
        if (user.getUserRoles().stream().anyMatch(r -> "ADMIN".equals(r.getId().getRoleCode()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot suspend an admin");
        }
        requireStatus(user, UserStatus.ACTIVE);
        user.setStatus(UserStatus.SUSPENDED);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", "SUSPENDED");
        after.put("reason", reason);
        audit.record(adminId, "USER_SUSPENDED", ENTITY_TYPE, userId, Map.of("status", "ACTIVE"), after);
        return AdminUserItem.of(user);
    }

    @Transactional
    public AdminUserItem reactivate(UUID adminId, UUID userId) {
        UserAccount user = lock(userId);
        requireStatus(user, UserStatus.SUSPENDED);
        user.setStatus(UserStatus.ACTIVE);
        audit.record(adminId, "USER_REACTIVATED", ENTITY_TYPE, userId, Map.of("status", "SUSPENDED"),
                Map.of("status", "ACTIVE"));
        return AdminUserItem.of(user);
    }

    private UserAccount lock(UUID userId) {
        return users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static void requireStatus(UserAccount user, UserStatus expected) {
        if (user.getStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is not " + expected.name());
        }
    }
}
