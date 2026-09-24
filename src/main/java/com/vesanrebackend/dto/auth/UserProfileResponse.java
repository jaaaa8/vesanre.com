package com.vesanrebackend.dto.auth;

import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        String phone,
        Set<String> roles,
        String providerStatus) {
}
