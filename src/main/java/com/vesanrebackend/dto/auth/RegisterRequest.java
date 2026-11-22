package com.vesanrebackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email(regexp = AuthPatterns.EMAIL) @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 32) @Pattern(regexp = AuthPatterns.PHONE) String phone) {
}
