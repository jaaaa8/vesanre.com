package com.vesanrebackend.dto.admin;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AuditLogItem(Long id, String action, String entityType, UUID entityId, Actor actor,
                           JsonNode beforeData, JsonNode afterData, Instant createdAt) {
    public record Actor(UUID id, String displayName) {
    }
}
