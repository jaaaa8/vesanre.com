package com.vesanrebackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProviderApplicationRequest(
        @NotBlank @Size(max = 200) String legalName,
        @Size(max = 50) String taxId,
        @Size(max = 160) String shopName,
        @Size(max = 2000) String shopDescription) {
}
