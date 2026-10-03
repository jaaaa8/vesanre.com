package com.vesanrebackend.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminProviderResponse(
        UUID userId,
        String email,
        String displayName,
        String phone,
        String legalName,
        String taxId,
        String status,
        Instant createdAt,
        ShopSummary shop,
        VerificationSummary verification) {

    public record ShopSummary(UUID id, String name, String slug, String status) {
    }

    public record VerificationSummary(UUID id, String status, String documents, Instant createdAt) {
    }
}
