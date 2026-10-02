package com.vesanrebackend.service.auth;

import com.vesanrebackend.dto.auth.LoginRequest;
import com.vesanrebackend.dto.auth.ProviderRegisterRequest;
import com.vesanrebackend.dto.auth.RegisterRequest;
import com.vesanrebackend.dto.auth.UpdateProfileRequest;
import com.vesanrebackend.dto.auth.UserProfileResponse;
import com.vesanrebackend.entity.ProviderProfile;
import com.vesanrebackend.entity.ProviderVerification;
import com.vesanrebackend.entity.Role;
import com.vesanrebackend.entity.Shop;
import com.vesanrebackend.entity.UserAccount;
import com.vesanrebackend.entity.UserRole;
import com.vesanrebackend.entity.enums.ProviderStatus;
import com.vesanrebackend.entity.enums.ShopStatus;
import com.vesanrebackend.entity.enums.UserStatus;
import com.vesanrebackend.entity.enums.VerificationStatus;
import com.vesanrebackend.repository.ProviderProfileRepository;
import com.vesanrebackend.repository.ProviderVerificationRepository;
import com.vesanrebackend.repository.RoleRepository;
import com.vesanrebackend.repository.ShopRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserRoleRepository userRoles = mock(UserRoleRepository.class);
    private final ProviderProfileRepository providerProfiles = mock(ProviderProfileRepository.class);
    private final ShopRepository shops = mock(ShopRepository.class);
    private final ProviderVerificationRepository verifications = mock(ProviderVerificationRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthService service = new AuthService(users, roles, userRoles, providerProfiles, shops, verifications, passwordEncoder);

    @Test
    void registersProviderWithProfileDraftShopAndPendingVerification() {
        stubRegistration("PROVIDER");
        when(providerProfiles.save(any(ProviderProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = service.registerProvider(new ProviderRegisterRequest(
                "Owner@Example.com", "correct-password", "Owner", null, "Công ty Đồng Phục", null, null, null));

        assertThat(response.email()).isEqualTo("Owner@Example.com");
        assertThat(response.roles()).containsExactly("PROVIDER");
        assertThat(response.providerStatus()).isEqualTo("PENDING");
        verify(userRoles).save(any());

        ArgumentCaptor<ProviderProfile> profile = ArgumentCaptor.forClass(ProviderProfile.class);
        verify(providerProfiles).save(profile.capture());
        assertThat(profile.getValue().getLegalName()).isEqualTo("Công ty Đồng Phục");
        assertThat(profile.getValue().getStatus()).isEqualTo(ProviderStatus.PENDING);

        ArgumentCaptor<Shop> shop = ArgumentCaptor.forClass(Shop.class);
        verify(shops).save(shop.capture());
        assertThat(shop.getValue().getOwner()).isSameAs(profile.getValue());
        assertThat(shop.getValue().getStatus()).isEqualTo(ShopStatus.DRAFT);
        assertThat(shop.getValue().getName()).isEqualTo("Công ty Đồng Phục");
        assertThat(shop.getValue().getSlug()).matches("cong-ty-dong-phuc-[a-z0-9]{6}");
        assertThat(shop.getValue().getDefaultCancellationPolicy()).isEqualTo("{}");

        ArgumentCaptor<ProviderVerification> verification = ArgumentCaptor.forClass(ProviderVerification.class);
        verify(verifications).save(verification.capture());
        assertThat(verification.getValue().getProvider()).isSameAs(profile.getValue());
        assertThat(verification.getValue().getSubmittedBy().getEmailNormalized()).isEqualTo("owner@example.com");
        assertThat(verification.getValue().getStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(verification.getValue().getDocuments()).isEqualTo("[]");
    }

    @Test
    void shopNameOverridesLegalNameWhenProvided() {
        stubRegistration("PROVIDER");
        when(providerProfiles.save(any(ProviderProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerProvider(new ProviderRegisterRequest(
                "owner@example.com", "correct-password", "Owner", null, "Legal Co", null, "Sân Bóng A", "desc"));

        ArgumentCaptor<Shop> shop = ArgumentCaptor.forClass(Shop.class);
        verify(shops).save(shop.capture());
        assertThat(shop.getValue().getName()).isEqualTo("Sân Bóng A");
        assertThat(shop.getValue().getSlug()).matches("san-bong-a-[a-z0-9]{6}");
    }

    @Test
    void registersCustomerWhenRoleIsMissingOrCustomer() {
        stubRegistration("CUSTOMER");

        UserProfileResponse noRole = service.register(new RegisterRequest("owner@example.com", "correct-password", "Owner", null, null));
        assertThat(noRole.roles()).containsExactly("CUSTOMER");
        assertThat(noRole.providerStatus()).isNull();

        service.register(new RegisterRequest("owner@example.com", "correct-password", "Owner", null, "customer"));
        verifyNoInteractions(providerProfiles, shops, verifications);
    }

    @Test
    void rejectsProviderAndAdminOnCustomerRegistration() {
        for (String role : new String[]{"PROVIDER", "ADMIN"}) {
            assertThatThrownBy(() -> service.register(new RegisterRequest(
                    "admin@example.com", "correct-password", "Admin", null, role)))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Providers must register via /api/auth/register/provider")
                    .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                    .isEqualTo(400);
        }
        verifyNoInteractions(users, roles, userRoles, providerProfiles, shops, verifications, passwordEncoder);
    }

    @Test
    void loginNormalizesEmailAndRejectsWrongPasswordOrInactiveAccount() {
        UserAccount active = user("Member@Example.com", UserStatus.ACTIVE, "CUSTOMER");
        when(users.findByEmailNormalized("member@example.com")).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        UserProfileResponse response = service.login(new LoginRequest("MEMBER@EXAMPLE.COM", "correct-password"));

        assertThat(response.email()).isEqualTo("Member@Example.com");
        verify(passwordEncoder).matches("correct-password", "hashed");
        verifyNoInteractions(providerProfiles);

        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);
        assertStatus(() -> service.login(new LoginRequest("member@example.com", "wrong-password")), 401);

        UserAccount inactive = user("inactive@example.com", UserStatus.SUSPENDED, "CUSTOMER");
        inactive.setPasswordHash("inactive-hash");
        when(users.findByEmailNormalized("inactive@example.com")).thenReturn(Optional.of(inactive));
        assertStatus(() -> service.login(new LoginRequest("inactive@example.com", "correct-password")), 401);
        verify(passwordEncoder, never()).matches("correct-password", "inactive-hash");
    }

    @Test
    void loginForUnknownEmailStillRunsPasswordCheckBefore401() {
        when(users.findByEmailNormalized("ghost@example.com")).thenReturn(Optional.empty());

        assertStatus(() -> service.login(new LoginRequest("ghost@example.com", "correct-password")), 401);

        verify(passwordEncoder).matches(anyString(), anyString());
    }

    @Test
    void pendingProviderGets403OnlyAfterCorrectPassword() {
        UserAccount provider = user("owner@example.com", UserStatus.ACTIVE, "PROVIDER");
        when(users.findByEmailNormalized("owner@example.com")).thenReturn(Optional.of(provider));
        when(providerProfiles.findById(provider.getId())).thenReturn(Optional.of(profile(provider, ProviderStatus.PENDING)));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("owner@example.com", "correct-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Provider account is pending approval")
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(403);
        assertStatus(() -> service.login(new LoginRequest("owner@example.com", "wrong-password")), 401);
    }

    @Test
    void rejectedAndSuspendedProvidersGetDistinctMessages() {
        UserAccount provider = user("owner@example.com", UserStatus.ACTIVE, "PROVIDER");
        when(users.findByEmailNormalized("owner@example.com")).thenReturn(Optional.of(provider));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        when(providerProfiles.findById(provider.getId())).thenReturn(Optional.of(profile(provider, ProviderStatus.REJECTED)));
        assertThatThrownBy(() -> service.login(new LoginRequest("owner@example.com", "correct-password")))
                .hasMessageContaining("Provider account was rejected");

        when(providerProfiles.findById(provider.getId())).thenReturn(Optional.of(profile(provider, ProviderStatus.SUSPENDED)));
        assertThatThrownBy(() -> service.login(new LoginRequest("owner@example.com", "correct-password")))
                .hasMessageContaining("Provider account is suspended");
    }

    @Test
    void verifiedProviderCanLogIn() {
        UserAccount provider = user("owner@example.com", UserStatus.ACTIVE, "PROVIDER");
        when(users.findByEmailNormalized("owner@example.com")).thenReturn(Optional.of(provider));
        when(providerProfiles.findById(provider.getId())).thenReturn(Optional.of(profile(provider, ProviderStatus.VERIFIED)));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        UserProfileResponse response = service.login(new LoginRequest("owner@example.com", "correct-password"));

        assertThat(response.roles()).containsExactly("PROVIDER");
        assertThat(response.providerStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void updateProfileRejectsWhitespaceOnlyPhoneButAllowsClearing() {
        UserAccount member = user("member@example.com", UserStatus.ACTIVE, "CUSTOMER");
        member.setPhone("0900000000");
        when(users.findById(member.getId())).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.updateProfile(member.getId(), new UpdateProfileRequest(null, "   ")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("phone cannot be blank")
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(400);
        assertThat(member.getPhone()).isEqualTo("0900000000");

        assertThat(service.updateProfile(member.getId(), new UpdateProfileRequest(null, "")).phone()).isNull();
    }

    @Test
    void profileOfInactiveUserIsForbidden() {
        UserAccount suspended = user("member@example.com", UserStatus.SUSPENDED, "CUSTOMER");
        when(users.findById(suspended.getId())).thenReturn(Optional.of(suspended));

        assertStatus(() -> service.profile(suspended.getId()), 403);
    }

    private void stubRegistration(String roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        when(users.existsByEmailNormalized("owner@example.com")).thenReturn(false);
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(roles.findById(roleCode)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("correct-password")).thenReturn("hashed");
    }

    private void assertStatus(Runnable call, int status) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(status);
    }

    private ProviderProfile profile(UserAccount user, ProviderStatus status) {
        ProviderProfile profile = new ProviderProfile();
        profile.setUser(user);
        profile.setUserId(user.getId());
        profile.setStatus(status);
        return profile;
    }

    private UserAccount user(String email, UserStatus status, String roleCode) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setEmailNormalized(email.toLowerCase());
        user.setPasswordHash("hashed");
        user.setDisplayName("Member");
        user.setStatus(status);
        Role role = new Role();
        role.setCode(roleCode);
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        user.getUserRoles().add(userRole);
        return user;
    }
}
