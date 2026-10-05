package com.vesanrebackend.service.admin;

import com.vesanrebackend.dto.admin.AdminProviderResponse;
import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.entity.provider.ProviderProfile;
import com.vesanrebackend.entity.provider.ProviderVerification;
import com.vesanrebackend.entity.account.Role;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.entity.account.UserAccount;
import com.vesanrebackend.entity.account.UserRole;
import com.vesanrebackend.entity.enums.ProviderStatus;
import com.vesanrebackend.entity.enums.ShopStatus;
import com.vesanrebackend.entity.enums.VerificationStatus;
import com.vesanrebackend.repository.ProviderProfileRepository;
import com.vesanrebackend.repository.ProviderVerificationRepository;
import com.vesanrebackend.repository.RoleRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.repository.UserRoleRepository;
import com.vesanrebackend.service.mail.ReviewMailer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminProviderService {
    private static final String ENTITY_TYPE = "PROVIDER_PROFILE";
    private static final String PROVIDER = "PROVIDER";

    private final ProviderProfileRepository providerProfiles;
    private final ProviderVerificationRepository verifications;
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final AdminAudit audit;
    private final Clock clock;
    private final ReviewMailer mailer;

    public AdminProviderService(ProviderProfileRepository providerProfiles, ProviderVerificationRepository verifications,
                                UserAccountRepository users, RoleRepository roles, UserRoleRepository userRoles,
                                AdminAudit audit, Clock clock,
                                ReviewMailer mailer) {
        this.providerProfiles = providerProfiles;
        this.verifications = verifications;
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.audit = audit;
        this.clock = clock;
        this.mailer = mailer;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProviderResponse> list(ProviderStatus status, Pageable pageable) {
        Page<ProviderProfile> page = providerProfiles.findByStatusWithDetails(status, pageable);
        Map<UUID, ProviderVerification> latest = new HashMap<>();
        if (page.hasContent()) {
            for (ProviderVerification verification : verifications.findByProviderIdsNewestFirst(
                    page.getContent().stream().map(ProviderProfile::getUserId).toList())) {
                latest.putIfAbsent(verification.getProvider().getUserId(), verification);
            }
        }
        return PageResponse.of(page.map(profile -> toResponse(profile, latest.get(profile.getUserId()))));
    }

    @Transactional
    public AdminProviderResponse approve(UUID adminId, UUID providerId) {
        ProviderProfile profile = lockPending(providerId);
        ProviderVerification verification = pendingVerification(providerId);
        Instant now = clock.instant();
        profile.setStatus(ProviderStatus.VERIFIED);
        profile.getShop().setStatus(ShopStatus.ACTIVE);
        profile.setVerifiedAt(now);
        review(verification, VerificationStatus.APPROVED, adminId, now, null);
        grantProviderRole(profile.getUser(), adminId);
        audit.record(adminId, "PROVIDER_APPROVED", ENTITY_TYPE, providerId, Map.of("status", "PENDING"), Map.of("status", "VERIFIED"));
        mailer.send(profile.getUser().getEmail(), "[Vesanre] Đơn đăng ký nhà cung cấp đã được duyệt",
                "Đơn đăng ký nhà cung cấp của bạn đã được duyệt. Bạn có thể đăng nhập lại để quản lý cửa hàng.", null);
        return toResponse(profile, verification);
    }

    @Transactional
    public AdminProviderResponse reject(UUID adminId, UUID providerId, String reason) {
        ProviderProfile profile = lockPending(providerId);
        ProviderVerification verification = pendingVerification(providerId);
        profile.setStatus(ProviderStatus.REJECTED);
        profile.setVerifiedAt(null);
        review(verification, VerificationStatus.REJECTED, adminId, clock.instant(), reason);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", "REJECTED");
        after.put("reason", reason);
        audit.record(adminId, "PROVIDER_REJECTED", ENTITY_TYPE, providerId, Map.of("status", "PENDING"), after);
        mailer.send(profile.getUser().getEmail(), "[Vesanre] Đơn đăng ký nhà cung cấp bị từ chối",
                "Đơn đăng ký nhà cung cấp của bạn đã bị từ chối.", reason);
        return toResponse(profile, verification);
    }

    // The row lock makes a second admin wait, then see the new status and get 409.
    private ProviderProfile lockPending(UUID providerId) {
        ProviderProfile profile = providerProfiles.findByIdForUpdate(providerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found"));
        if (profile.getStatus() != ProviderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Provider is not pending");
        }
        return profile;
    }

    private ProviderVerification pendingVerification(UUID providerId) {
        return verifications.findByProviderAndStatus(providerId, VerificationStatus.PENDING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Provider has no pending verification"));
    }

    private void grantProviderRole(UserAccount user, UUID adminId) {
        if (user.getUserRoles().stream().anyMatch(userRole -> PROVIDER.equals(userRole.getRole().getCode()))) {
            return;
        }
        Role role = roles.findById(PROVIDER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role seed is missing"));
        UserRole userRole = new UserRole();
        userRole.setId(new UserRole.UserRoleId(user.getId(), PROVIDER));
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setGrantedBy(users.getReferenceById(adminId));
        userRoles.save(userRole);
        user.getUserRoles().add(userRole);
    }

    private void review(ProviderVerification verification, VerificationStatus status, UUID adminId, Instant now, String reason) {
        verification.setStatus(status);
        verification.setReviewedBy(users.getReferenceById(adminId));
        verification.setReviewedAt(now);
        verification.setRejectionReason(reason);
    }

    private AdminProviderResponse toResponse(ProviderProfile profile, ProviderVerification verification) {
        UserAccount user = profile.getUser();
        Shop shop = profile.getShop();
        return new AdminProviderResponse(profile.getUserId(), user.getEmail(), user.getDisplayName(), user.getPhone(),
                profile.getLegalName(), profile.getTaxId(), profile.getStatus().name(), profile.getCreatedAt(),
                shop == null ? null : new AdminProviderResponse.ShopSummary(shop.getId(), shop.getName(), shop.getSlug(), shop.getStatus().name()),
                verification == null ? null : new AdminProviderResponse.VerificationSummary(verification.getId(),
                        verification.getStatus().name(), verification.getDocuments(), verification.getCreatedAt()));
    }
}
