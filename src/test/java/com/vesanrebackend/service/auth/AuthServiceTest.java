package com.vesanrebackend.service.auth;

import com.vesanrebackend.dto.auth.LoginRequest;
import com.vesanrebackend.dto.auth.ProviderApplicationRequest;
import com.vesanrebackend.dto.auth.RegisterRequest;
import com.vesanrebackend.dto.auth.UpdateProfileRequest;
import com.vesanrebackend.dto.auth.UserProfileResponse;
import com.vesanrebackend.entity.provider.ProviderProfile;
import com.vesanrebackend.entity.provider.ProviderVerification;
import com.vesanrebackend.entity.account.Role;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.entity.account.UserAccount;
import com.vesanrebackend.entity.account.UserRole;
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

import java.time.Instant;
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

/**
 * Loại test: unit - AuthService với repository và PasswordEncoder được mock (không HTTP, không DB).
 * Dùng bởi POST /api/auth/register, POST /api/auth/login, GET/PATCH /api/profile/me, POST /api/profile/provider-application.
 */
class AuthServiceTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserRoleRepository userRoles = mock(UserRoleRepository.class);
    private final ProviderProfileRepository providerProfiles = mock(ProviderProfileRepository.class);
    private final ShopRepository shops = mock(ShopRepository.class);
    private final ProviderVerificationRepository verifications = mock(ProviderVerificationRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthService service = new AuthService(users, roles, userRoles, providerProfiles, shops, verifications, passwordEncoder);

    // Thành phần: AuthService.applyProvider (POST /api/profile/provider-application)
    // Kiểm tra: Tạo profile PENDING, shop DRAFT (tên/slug đúng) và verification PENDING; user giữ role CUSTOMER, không cấp PROVIDER.
    @Test
    void applyCreatesPendingProfileDraftShopAndPendingVerification() {
        UserAccount applicant = user("owner@example.com", UserStatus.ACTIVE, "CUSTOMER");
        stubApplicant(applicant, null);

        UserProfileResponse response = service.applyProvider(applicant.getId(),
                new ProviderApplicationRequest("Công ty Đồng Phục", null, null, null));

        assertThat(response.roles()).containsExactly("CUSTOMER");
        assertThat(response.providerStatus()).isEqualTo("PENDING");
        verifyNoInteractions(userRoles);

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
        assertThat(verification.getValue().getSubmittedBy()).isSameAs(applicant);
        assertThat(verification.getValue().getStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(verification.getValue().getDocuments()).isEqualTo("[]");
    }

    // Thành phần: AuthService.applyProvider (POST /api/profile/provider-application)
    // Kiểm tra: Có shopName thì dùng làm tên shop thay cho legalName.
    @Test
    void shopNameOverridesLegalNameWhenProvided() {
        UserAccount applicant = user("owner@example.com", UserStatus.ACTIVE, "CUSTOMER");
        stubApplicant(applicant, null);

        service.applyProvider(applicant.getId(), new ProviderApplicationRequest("Legal Co", null, "Sân Bóng A", "desc"));

        ArgumentCaptor<Shop> shop = ArgumentCaptor.forClass(Shop.class);
        verify(shops).save(shop.capture());
        assertThat(shop.getValue().getName()).isEqualTo("Sân Bóng A");
        assertThat(shop.getValue().getSlug()).matches("san-bong-a-[a-z0-9]{6}");
    }

    // Thành phần: AuthService.applyProvider (POST /api/profile/provider-application)
    // Kiểm tra: Hồ sơ REJECTED nộp lại: về PENDING, xóa verifiedAt, cập nhật thông tin, giữ slug shop, thêm verification mới.
    @Test
    void reapplyAfterRejectionResetsToPendingKeepsSlugAndAddsVerification() {
        UserAccount applicant = user("owner@example.com", UserStatus.ACTIVE, "CUSTOMER");
        ProviderProfile existing = profile(applicant, ProviderStatus.REJECTED);
        Shop shop = new Shop();
        shop.setName("Old");
        shop.setSlug("old-abc123");
        shop.setOwner(existing);
        existing.setShop(shop);
        existing.setVerifiedAt(Instant.parse("2026-01-01T00:00:00Z"));
        stubApplicant(applicant, existing);

        UserProfileResponse response = service.applyProvider(applicant.getId(),
                new ProviderApplicationRequest(" New Legal ", "TAX1", "New Shop", "new desc"));

        assertThat(response.providerStatus()).isEqualTo("PENDING");
        assertThat(existing.getStatus()).isEqualTo(ProviderStatus.PENDING);
        assertThat(existing.getVerifiedAt()).isNull();
        assertThat(existing.getLegalName()).isEqualTo("New Legal");
        assertThat(existing.getTaxId()).isEqualTo("TAX1");
        assertThat(shop.getName()).isEqualTo("New Shop");
        assertThat(shop.getDescription()).isEqualTo("new desc");
        assertThat(shop.getSlug()).isEqualTo("old-abc123");
        verify(providerProfiles, never()).save(any());
        verify(shops, never()).save(any());
        ArgumentCaptor<ProviderVerification> verification = ArgumentCaptor.forClass(ProviderVerification.class);
        verify(verifications).save(verification.capture());
        assertThat(verification.getValue().getProvider()).isSameAs(existing);
        assertThat(verification.getValue().getStatus()).isEqualTo(VerificationStatus.PENDING);
    }

    // Thành phần: AuthService.applyProvider (POST /api/profile/provider-application)
    // Kiểm tra: PENDING/VERIFIED/SUSPENDED nộp đơn trả 409 và không tạo shop/verification.
    @Test
    void applyIsConflictWhenPendingVerifiedOrSuspended() {
        for (ProviderStatus status : new ProviderStatus[]{ProviderStatus.PENDING, ProviderStatus.VERIFIED, ProviderStatus.SUSPENDED}) {
            UserAccount applicant = user("owner@example.com", UserStatus.ACTIVE, "CUSTOMER");
            stubApplicant(applicant, profile(applicant, status));

            assertStatus(() -> service.applyProvider(applicant.getId(), new ProviderApplicationRequest("Legal", null, null, null)), 409);
        }
        verifyNoInteractions(shops, verifications);
    }

    // Thành phần: AuthService.register (POST /api/auth/register)
    // Kiểm tra: Đăng ký chỉ tạo CUSTOMER, không tạo hồ sơ provider/shop/verification.
    @Test
    void registerAlwaysCreatesCustomerOnly() {
        stubRegistration("CUSTOMER");

        UserProfileResponse response = service.register(new RegisterRequest("owner@example.com", "correct-password", "Owner", null));

        assertThat(response.roles()).containsExactly("CUSTOMER");
        assertThat(response.providerStatus()).isNull();
        verifyNoInteractions(providerProfiles, shops, verifications);
    }

    // Thành phần: AuthService.login (POST /api/auth/login)
    // Kiểm tra: Chuẩn hóa email khi tìm; sai mật khẩu hoặc tài khoản SUSPENDED trả 401.
    @Test
    void loginNormalizesEmailAndRejectsWrongPasswordOrInactiveAccount() {
        UserAccount active = user("Member@Example.com", UserStatus.ACTIVE, "CUSTOMER");
        when(users.findByEmailNormalized("member@example.com")).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        UserProfileResponse response = service.login(new LoginRequest("MEMBER@EXAMPLE.COM", "correct-password"));

        assertThat(response.email()).isEqualTo("Member@Example.com");
        verify(passwordEncoder).matches("correct-password", "hashed");
        assertThat(response.providerStatus()).isNull();

        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);
        assertStatus(() -> service.login(new LoginRequest("member@example.com", "wrong-password")), 401);

        UserAccount inactive = user("inactive@example.com", UserStatus.SUSPENDED, "CUSTOMER");
        inactive.setPasswordHash("inactive-hash");
        when(users.findByEmailNormalized("inactive@example.com")).thenReturn(Optional.of(inactive));
        assertStatus(() -> service.login(new LoginRequest("inactive@example.com", "correct-password")), 401);
        verify(passwordEncoder, never()).matches("correct-password", "inactive-hash");
    }

    // Thành phần: AuthService.login (POST /api/auth/login)
    // Kiểm tra: Email không tồn tại vẫn gọi password check rồi mới trả 401 (xác nhận lời gọi, không đo thời gian).
    @Test
    void loginForUnknownEmailStillRunsPasswordCheckBefore401() {
        when(users.findByEmailNormalized("ghost@example.com")).thenReturn(Optional.empty());

        assertStatus(() -> service.login(new LoginRequest("ghost@example.com", "correct-password")), 401);

        verify(passwordEncoder).matches(anyString(), anyString());
    }

    // Thành phần: AuthService.login (POST /api/auth/login)
    // Kiểm tra: CUSTOMER có hồ sơ PENDING vẫn đăng nhập được và nhận providerStatus PENDING.
    @Test
    void customerWithPendingApplicationCanLogInAndSeesStatus() {
        UserAccount applicant = user("owner@example.com", UserStatus.ACTIVE, "CUSTOMER");
        when(users.findByEmailNormalized("owner@example.com")).thenReturn(Optional.of(applicant));
        when(providerProfiles.findById(applicant.getId())).thenReturn(Optional.of(profile(applicant, ProviderStatus.PENDING)));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        UserProfileResponse response = service.login(new LoginRequest("owner@example.com", "correct-password"));

        assertThat(response.roles()).containsExactly("CUSTOMER");
        assertThat(response.providerStatus()).isEqualTo("PENDING");
    }

    // Thành phần: AuthService.login (POST /api/auth/login)
    // Kiểm tra: Provider VERIFIED đăng nhập được, nhận role và trạng thái đúng.
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

    // Thành phần: AuthService.updateProfile (PATCH /api/profile/me)
    // Kiểm tra: Phone toàn khoảng trắng bị từ chối 400 và giữ giá trị cũ; chuỗi rỗng xóa phone về null.
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

    // Thành phần: AuthService.profile (GET /api/profile/me và các API profile)
    // Kiểm tra: Tài khoản SUSPENDED đọc hồ sơ bị trả 403.
    @Test
    void profileOfInactiveUserIsForbidden() {
        UserAccount suspended = user("member@example.com", UserStatus.SUSPENDED, "CUSTOMER");
        when(users.findById(suspended.getId())).thenReturn(Optional.of(suspended));

        assertStatus(() -> service.profile(suspended.getId()), 403);
    }

    private void stubApplicant(UserAccount applicant, ProviderProfile existing) {
        when(users.findById(applicant.getId())).thenReturn(Optional.of(applicant));
        when(providerProfiles.findByIdForUpdate(applicant.getId())).thenReturn(Optional.ofNullable(existing));
        when(providerProfiles.save(any(ProviderProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
