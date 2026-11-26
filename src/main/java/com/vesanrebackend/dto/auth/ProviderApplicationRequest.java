package com.vesanrebackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProviderApplicationRequest(
        @NotBlank @Pattern(regexp = AuthPatterns.LEGAL_NAME) String legalName,
        @Pattern(regexp = AuthPatterns.TAX_ID) String taxId,
        @Pattern(regexp = AuthPatterns.SHOP_NAME) String shopName,
        @Size(max = 2000) String shopDescription) {
}
