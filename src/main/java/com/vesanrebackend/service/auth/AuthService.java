package com.vesanrebackend.service.auth;

import com.vesanrebackend.dto.auth.LoginRequest;
import com.vesanrebackend.dto.auth.RegisterRequest;
import com.vesanrebackend.dto.auth.UpdateProfileRequest;
import com.vesanrebackend.dto.auth.UserProfileResponse;
import com.vesanrebackend.entity.ProviderProfile;
import com.vesanrebackend.entity.Role;
import com.vesanrebackend.entity.UserAccount;
import com.vesanrebackend.entity.UserRole;
import com.vesanrebackend.entity.enums.UserStatus;
import com.vesanrebackend.repository.ProviderProfileRepository;
import com.vesanrebackend.repository.RoleRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.repository.UserRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private static final String CUSTOMER = "CUSTOMER";
    private static final String PROVIDER = "PROVIDER";

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final ProviderProfileRepository providerProfiles;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountRepository users, RoleRepository roles, UserRoleRepository userRoles,
                       ProviderProfileRepository providerProfiles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.providerProfiles = providerProfiles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String email = request.email().trim();
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        String roleCode = request.role().trim().toUpperCase(Locale.ROOT);
        if (!CUSTOMER.equals(roleCode) && !PROVIDER.equals(roleCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only CUSTOMER or PROVIDER can self-register");
        }
        if (PROVIDER.equals(roleCode) && (request.legalName() == null || request.legalName().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "legalName is required for PROVIDER");
        }
        if (users.existsByEmailNormalized(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setEmailNormalized(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setPhone(normalizeOptional(request.phone()));
        user = users.save(user);

        Role role = roles.findById(roleCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role seed is missing"));
        UserRole userRole = new UserRole();
        userRole.setId(new UserRole.UserRoleId(user.getId(), roleCode));
        userRole.setUser(user);
        userRole.setRole(role);
        userRoles.save(userRole);
        user.getUserRoles().add(userRole);

        if (PROVIDER.equals(roleCode)) {
            ProviderProfile providerProfile = new ProviderProfile();
            providerProfile.setUser(user);
            providerProfile.setLegalName(request.legalName().trim());
            providerProfile.setTaxId(normalizeOptional(request.taxId()));
            providerProfiles.save(providerProfile);
            user.setProviderProfile(providerProfile);
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse login(LoginRequest request) {
        UserAccount user = users.findByEmailNormalized(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(UUID userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        if (request.displayName() == null && request.phone() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide displayName or phone");
        }
        UserAccount user = findUser(userId);
        if (request.displayName() != null) {
            if (request.displayName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName cannot be blank");
            }
            user.setDisplayName(request.displayName().trim());
        }
        if (request.phone() != null) {
            String phone = normalizeOptional(request.phone());
            if (phone != null && users.existsByPhoneAndIdNot(phone, userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
            }
            user.setPhone(phone);
        }
        return toResponse(user);
    }

    private UserAccount findUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfileResponse toResponse(UserAccount user) {
        String providerStatus = user.getProviderProfile() == null ? null : user.getProviderProfile().getStatus().name();
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getPhone(),
                user.getUserRoles().stream().map(userRole -> userRole.getRole().getCode()).collect(Collectors.toUnmodifiableSet()),
                providerStatus);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
