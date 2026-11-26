package com.vesanrebackend.dto.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Pattern(regexp = AuthPatterns.NAME) String displayName,
        @Size(max = 32) @Pattern(regexp = AuthPatterns.PHONE) String phone) {
}
