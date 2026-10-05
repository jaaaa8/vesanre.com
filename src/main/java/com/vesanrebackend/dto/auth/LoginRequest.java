package com.vesanrebackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email(regexp = AuthPatterns.EMAIL) @Size(max = 320) String email,
        @NotBlank @Size(max = 72) String password) {
}
