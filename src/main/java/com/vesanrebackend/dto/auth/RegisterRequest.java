package com.vesanrebackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email(regexp = AuthPatterns.EMAIL) @Size(max = 320) String email,
        @NotBlank @Pattern(regexp = AuthPatterns.PASSWORD) String password,
        @NotBlank @Pattern(regexp = AuthPatterns.NAME) String displayName,
        @Size(max = 32) @Pattern(regexp = AuthPatterns.PHONE) String phone) {
}
