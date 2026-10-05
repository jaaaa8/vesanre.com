package com.vesanrebackend.dto.admin;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AdminChangeRequestResponse(UUID id, String targetType, UUID targetId, String targetName,
                                         Map<String, Object> current, Map<String, Object> proposed,
                                         String submittedBy, String status, Instant createdAt) {
}
