package com.vesanrebackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 32) String phone,
        @NotBlank @Size(max = 30) String role,
        @Size(max = 200) String legalName,
        @Size(max = 50) String taxId) {
}
