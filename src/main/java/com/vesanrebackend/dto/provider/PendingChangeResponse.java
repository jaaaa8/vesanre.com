package com.vesanrebackend.dto.provider;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PendingChangeResponse(UUID id, Map<String, Object> proposed, Instant createdAt) {
}
