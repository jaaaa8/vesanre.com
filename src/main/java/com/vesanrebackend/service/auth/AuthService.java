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
import com.vesanrebackend.entity.enums.UserStatus;
import com.vesanrebackend.repository.ProviderProfileRepository;
import com.vesanrebackend.repository.ProviderVerificationRepository;
import com.vesanrebackend.repository.RoleRepository;
import com.vesanrebackend.repository.ShopRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.repository.UserRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private static final String CUSTOMER = "CUSTOMER";
    private static final String PROVIDER = "PROVIDER";
    private static final String SLUG_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    // Valid BCrypt hash of a throwaway string; unknown emails are checked against it so response time does not reveal them.
    private static final String DUMMY_HASH = "$2a$10$6n9Hnw/SGAsp6GlpcQ3oF.yIun.C0qxpd6dTqwPNhCX27Y8mO5mjG";

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final ProviderProfileRepository providerProfiles;
    private final ShopRepository shops;
    private final ProviderVerificationRepository providerVerifications;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountRepository users, RoleRepository roles, UserRoleRepository userRoles,
                       ProviderProfileRepository providerProfiles, ShopRepository shops,
                       ProviderVerificationRepository providerVerifications, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.providerProfiles = providerProfiles;
        this.shops = shops;
        this.providerVerifications = providerVerifications;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String roleCode = request.role() == null || request.role().isBlank()
                ? CUSTOMER : request.role().trim().toUpperCase(Locale.ROOT);
        if (!CUSTOMER.equals(roleCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Providers must register via /api/auth/register/provider");
        }
        UserAccount user = createAccount(request.email(), request.password(), request.displayName(), request.phone(), CUSTOMER);
        return toResponse(user, null);
    }

    @Transactional
    public UserProfileResponse registerProvider(ProviderRegisterRequest request) {
        UserAccount user = createAccount(request.email(), request.password(), request.displayName(), request.phone(), PROVIDER);
        String legalName = request.legalName().trim();

        ProviderProfile providerProfile = new ProviderProfile();
        providerProfile.setUser(user);
        providerProfile.setLegalName(legalName);
        providerProfile.setTaxId(normalizeOptional(request.taxId()));
        providerProfile = providerProfiles.save(providerProfile);

        // Every provider owns exactly one shop from day one; it stays DRAFT until moderation approves it separately.
        String shopName = normalizeOptional(request.shopName());
        Shop shop = new Shop();
        shop.setOwner(providerProfile);
        String name = shopName != null ? shopName : legalName;
        shop.setName(name.substring(0, Math.min(name.length(), 160))); // legalName may exceed shops.name length
        shop.setSlug(generateSlug(shop.getName()));
        shop.setDescription(normalizeOptional(request.shopDescription()));
        shop.setDefaultCancellationPolicy("{}");
        shops.save(shop);

        ProviderVerification verification = new ProviderVerification();
        verification.setProvider(providerProfile);
        verification.setSubmittedBy(user);
        verification.setDocuments("[]");
        providerVerifications.save(verification);

        return toResponse(user, providerProfile.getStatus());
    }

    // Deliberately not @Transactional: BCrypt is slow and must not hold a DB connection while it runs.
    public UserProfileResponse login(LoginRequest request) {
        UserAccount user = users.findByEmailNormalized(request.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        ProviderStatus providerStatus = providerStatus(user);
        if (providerStatus != null && providerStatus != ProviderStatus.VERIFIED) {
            String reason = switch (providerStatus) {
                case REJECTED -> "was rejected";
                case SUSPENDED -> "is suspended";
                default -> "is pending approval";
            };
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Provider account " + reason);
        }
        return toResponse(user, providerStatus);
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
            // Exactly "" clears the phone; whitespace-only is almost certainly a client bug.
            if (!request.phone().isEmpty() && request.phone().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone cannot be blank");
            }
            String phone = normalizeOptional(request.phone());
            if (phone != null && users.existsByPhoneAndIdNot(phone, userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
            }
            user.setPhone(phone);
        }
        return toResponse(user);
    }

    private UserAccount createAccount(String rawEmail, String password, String displayName, String rawPhone, String roleCode) {
        String email = rawEmail.trim();
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        // BCrypt rejects more than 72 bytes; @Size counts chars, so multi-byte passwords can slip through.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must be at most 72 bytes");
        }
        if (users.existsByEmailNormalized(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        String phone = normalizeOptional(rawPhone);
        if (phone != null && users.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setEmailNormalized(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName.trim());
        user.setPhone(phone);
        user = users.save(user);

        Role role = roles.findById(roleCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role seed is missing"));
        UserRole userRole = new UserRole();
        userRole.setId(new UserRole.UserRoleId(user.getId(), roleCode));
        userRole.setUser(user);
        userRole.setRole(role);
        userRoles.save(userRole);
        user.getUserRoles().add(userRole);
        return user;
    }

    private UserAccount findUser(UUID userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        return user;
    }

    // Only providers have a profile row, so skip the lookup for everyone else.
    private ProviderStatus providerStatus(UserAccount user) {
        if (user.getUserRoles().stream().noneMatch(userRole -> PROVIDER.equals(userRole.getRole().getCode()))) {
            return null;
        }
        return providerProfiles.findById(user.getId()).map(ProviderProfile::getStatus).orElse(ProviderStatus.PENDING);
    }

    private UserProfileResponse toResponse(UserAccount user) {
        return toResponse(user, providerStatus(user));
    }

    private UserProfileResponse toResponse(UserAccount user, ProviderStatus providerStatus) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getPhone(),
                user.getUserRoles().stream().map(userRole -> userRole.getRole().getCode()).collect(Collectors.toUnmodifiableSet()),
                providerStatus == null ? null : providerStatus.name());
    }

    // Slug = accent-stripped lowercase name + random suffix, so two shops with the same name never collide.
    private String generateSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'd')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-");
        base = base.substring(0, Math.min(base.length(), 100)).replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            base = "shop";
        }
        StringBuilder suffix = new StringBuilder("-");
        for (int i = 0; i < 6; i++) {
            suffix.append(SLUG_CHARS.charAt(RANDOM.nextInt(SLUG_CHARS.length())));
        }
        return base + suffix;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
