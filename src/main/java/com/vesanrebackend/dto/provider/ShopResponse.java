package com.vesanrebackend.dto.provider;

import java.util.UUID;

public record ShopResponse(UUID id, String slug, String name, String description, String status,
                           PendingChangeResponse pendingChange, String logoUrl) {
}
