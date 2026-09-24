package com.vesanrebackend.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserProfileResponse user) {
}
