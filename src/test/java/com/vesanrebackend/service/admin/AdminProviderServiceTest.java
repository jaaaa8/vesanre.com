package com.vesanrebackend.service.admin;

import com.vesanrebackend.dto.admin.AdminProviderResponse;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminProviderServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");

    private final ProviderProfileRepository providerProfiles = mock(ProviderProfileRepository.class);
    private final ProviderVerificationRepository verifications = mock(ProviderVerificationRepository.class);
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserRoleRepository userRoles = mock(UserRoleRepository.class);
    private final AdminAudit audit = mock(AdminAudit.class);
    private final ReviewMailer mailer = mock(ReviewMailer.class);
    private final AdminProviderService service = new AdminProviderService(providerProfiles, verifications, users, roles, userRoles, audit,
            Clock.fixed(NOW, ZoneOffset.UTC), mailer);

    private final UUID adminId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();
    private final UserAccount admin = new UserAccount();
    private final ProviderProfile profile = profile(ProviderStatus.PENDING);
    private final Shop shop = profile.getShop();
    private final ProviderVerification verification = verification();

    @Test
    void approveVerifiesProfileApprovesVerificationActivatesShopAndAudits() {
        stubPending();

        AdminProviderResponse response = service.approve(adminId, providerId);

        assertThat(profile.getStatus()).isEqualTo(ProviderStatus.VERIFIED);
        assertThat(profile.getVerifiedAt()).isEqualTo(NOW);
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(verification.getReviewedBy()).isSameAs(admin);
        assertThat(verification.getReviewedAt()).isEqualTo(NOW);
        assertThat(verification.getRejectionReason()).isNull();
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.ACTIVE);
        assertThat(response.status()).isEqualTo("VERIFIED");
        assertThat(response.shop().status()).isEqualTo("ACTIVE");
        assertThat(response.verification().status()).isEqualTo("APPROVED");

        verify(audit).record(adminId, "PROVIDER_APPROVED", "PROVIDER_PROFILE", providerId, Map.of("status", "PENDING"),
                Map.of("status", "VERIFIED"));
    }

    @Test
    void approveGrantsProviderRoleOnceAndRejectGrantsNothing() {
        stubPending();

        service.approve(adminId, providerId);

        ArgumentCaptor<UserRole> saved = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoles).save(saved.capture());
        assertThat(saved.getValue().getId().getRoleCode()).isEqualTo("PROVIDER");
        assertThat(saved.getValue().getId().getUserId()).isEqualTo(providerId);
        assertThat(saved.getValue().getGrantedBy()).isSameAs(admin);
        assertThat(profile.getUser().getUserRoles()).containsExactly(saved.getValue());

        // Already holding the role: nothing more is inserted.
        profile.setStatus(ProviderStatus.PENDING);
        service.approve(adminId, providerId);
        verify(userRoles, times(1)).save(any());
    }

    @Test
    void rejectDoesNotGrantProviderRole() {
        stubPending();

        service.reject(adminId, providerId, "no");

        verifyNoInteractions(userRoles, roles);
    }

    @Test
    void rejectStoresReasonAndLeavesShopAndVerifiedAtUntouched() {
        stubPending();

        AdminProviderResponse response = service.reject(adminId, providerId, "Tax id \"x\" is invalid");

        assertThat(profile.getStatus()).isEqualTo(ProviderStatus.REJECTED);
        assertThat(profile.getVerifiedAt()).isNull();
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(verification.getRejectionReason()).isEqualTo("Tax id \"x\" is invalid");
        assertThat(verification.getReviewedBy()).isSameAs(admin);
        assertThat(verification.getReviewedAt()).isEqualTo(NOW);
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.DRAFT);
        assertThat(response.status()).isEqualTo("REJECTED");

        verify(audit).record(adminId, "PROVIDER_REJECTED", "PROVIDER_PROFILE", providerId, Map.of("status", "PENDING"),
                Map.of("status", "REJECTED", "reason", "Tax id \"x\" is invalid"));
    }

    @Test
    void approveAndRejectMailTheApplicant() {
        stubPending();
        service.approve(adminId, providerId);
        verify(mailer).send(eq("provider@example.com"), eq("[Vesanre] Đơn đăng ký nhà cung cấp đã được duyệt"), any(), isNull());

        profile.setStatus(ProviderStatus.PENDING);
        verification.setStatus(VerificationStatus.PENDING);
        service.reject(adminId, providerId, "Thiếu giấy tờ");
        verify(mailer).send(eq("provider@example.com"), eq("[Vesanre] Đơn đăng ký nhà cung cấp bị từ chối"), any(), eq("Thiếu giấy tờ"));
    }

    @Test
    void notPendingIsConflict() {
        profile.setStatus(ProviderStatus.VERIFIED);
        when(providerProfiles.findByIdForUpdate(providerId)).thenReturn(Optional.of(profile));

        assertStatus(() -> service.approve(adminId, providerId), 409, "Provider is not pending");
        assertStatus(() -> service.reject(adminId, providerId, "no"), 409, "Provider is not pending");
        verifyNoInteractions(audit);
        verifyNoInteractions(mailer);
    }

    @Test
    void unknownProviderIsNotFound() {
        when(providerProfiles.findByIdForUpdate(providerId)).thenReturn(Optional.empty());

        assertStatus(() -> service.approve(adminId, providerId), 404, "Provider not found");
        assertStatus(() -> service.reject(adminId, providerId, "no"), 404, "Provider not found");
    }

    @Test
    void missingPendingVerificationIsConflictAndChangesNothing() {
        when(providerProfiles.findByIdForUpdate(providerId)).thenReturn(Optional.of(profile));
        when(verifications.findByProviderAndStatus(providerId, VerificationStatus.PENDING)).thenReturn(Optional.empty());

        assertStatus(() -> service.approve(adminId, providerId), 409, "Provider has no pending verification");
        assertStatus(() -> service.reject(adminId, providerId, "no"), 409, "Provider has no pending verification");
        assertThat(profile.getStatus()).isEqualTo(ProviderStatus.PENDING);
        verifyNoInteractions(audit);
    }

    private void stubPending() {
        when(providerProfiles.findByIdForUpdate(providerId)).thenReturn(Optional.of(profile));
        when(verifications.findByProviderAndStatus(providerId, VerificationStatus.PENDING)).thenReturn(Optional.of(verification));
        when(users.getReferenceById(adminId)).thenReturn(admin);
        Role providerRole = new Role();
        providerRole.setCode("PROVIDER");
        when(roles.findById("PROVIDER")).thenReturn(Optional.of(providerRole));
    }

    private void assertStatus(Runnable call, int status, String reason) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode().value()).isEqualTo(status);
                    assertThat(error.getReason()).isEqualTo(reason);
                });
    }

    private ProviderProfile profile(ProviderStatus status) {
        UserAccount user = new UserAccount();
        user.setId(providerId);
        user.setEmail("provider@example.com");
        user.setDisplayName("Provider");
        ProviderProfile profile = new ProviderProfile();
        profile.setUserId(providerId);
        profile.setUser(user);
        profile.setLegalName("Legal");
        profile.setStatus(status);
        Shop shop = new Shop();
        shop.setId(UUID.randomUUID());
        shop.setName("Shop");
        shop.setSlug("shop-abc123");
        shop.setOwner(profile);
        profile.setShop(shop);
        return profile;
    }

    private ProviderVerification verification() {
        ProviderVerification verification = new ProviderVerification();
        verification.setId(UUID.randomUUID());
        verification.setProvider(profile);
        verification.setDocuments("[]");
        return verification;
    }
}
