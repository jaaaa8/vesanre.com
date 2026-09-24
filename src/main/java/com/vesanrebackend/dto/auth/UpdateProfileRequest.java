package com.vesanrebackend.dto.auth;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 120) String displayName,
        @Size(max = 32) String phone) {
}
