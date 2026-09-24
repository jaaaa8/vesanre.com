package com.vesanrebackend.service.auth;

import com.vesanrebackend.dto.auth.RegisterRequest;
import com.vesanrebackend.dto.auth.UserProfileResponse;
import com.vesanrebackend.entity.Role;
import com.vesanrebackend.entity.UserAccount;
import com.vesanrebackend.entity.enums.UserStatus;
import com.vesanrebackend.repository.ProviderProfileRepository;
import com.vesanrebackend.repository.RoleRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserRoleRepository userRoles = mock(UserRoleRepository.class);
    private final ProviderProfileRepository providerProfiles = mock(ProviderProfileRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthService service = new AuthService(users, roles, userRoles, providerProfiles, passwordEncoder);

    @Test
    void registersProviderWithOnlyProviderRoleAndPendingProfile() {
        Role provider = new Role();
        provider.setCode("PROVIDER");
        when(users.existsByEmailNormalized("owner@example.com")).thenReturn(false);
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(roles.findById("PROVIDER")).thenReturn(Optional.of(provider));
        when(passwordEncoder.encode("correct-password")).thenReturn("hashed");

        UserProfileResponse response = service.register(new RegisterRequest(
                "Owner@Example.com", "correct-password", "Owner", null, "provider", "Owner Sport", null));

        assertThat(response.email()).isEqualTo("Owner@Example.com");
        assertThat(response.roles()).containsExactly("PROVIDER");
        assertThat(response.providerStatus()).isEqualTo("PENDING");
        verify(passwordEncoder).encode("correct-password");
        verify(userRoles).save(any());
        ArgumentCaptor<com.vesanrebackend.entity.ProviderProfile> profile = ArgumentCaptor.forClass(com.vesanrebackend.entity.ProviderProfile.class);
        verify(providerProfiles).save(profile.capture());
        assertThat(profile.getValue().getLegalName()).isEqualTo("Owner Sport");
    }

    @Test
    void rejectsPublicAdminRegistration() {
        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "admin@example.com", "correct-password", "Admin", null, "ADMIN", null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(400);
        verifyNoInteractions(users, roles, userRoles, providerProfiles, passwordEncoder);
    }

    @Test
    void loginNormalizesEmailAndRejectsWrongPasswordOrInactiveAccount() {
        UserAccount active = user("Member@Example.com", UserStatus.ACTIVE);
        when(users.findByEmailNormalized("member@example.com")).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        UserProfileResponse response = service.login(new com.vesanrebackend.dto.auth.LoginRequest("MEMBER@EXAMPLE.COM", "correct-password"));

        assertThat(response.email()).isEqualTo("Member@Example.com");
        verify(passwordEncoder).matches("correct-password", "hashed");

        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);
        assertThatThrownBy(() -> service.login(new com.vesanrebackend.dto.auth.LoginRequest("member@example.com", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(401);

        UserAccount inactive = user("inactive@example.com", UserStatus.SUSPENDED);
        inactive.setPasswordHash("inactive-hash");
        when(users.findByEmailNormalized("inactive@example.com")).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.login(new com.vesanrebackend.dto.auth.LoginRequest("inactive@example.com", "correct-password")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(401);
        verify(passwordEncoder, never()).matches("correct-password", "inactive-hash");
    }

    private UserAccount user(String email, UserStatus status) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setEmailNormalized(email.toLowerCase());
        user.setPasswordHash("hashed");
        user.setDisplayName("Member");
        user.setStatus(status);
        return user;
    }
}
