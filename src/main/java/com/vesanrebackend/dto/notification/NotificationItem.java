package com.vesanrebackend.dto.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationItem(UUID id, String type, String title, String body, UUID bookingId, Instant readAt,
                               Instant createdAt) {
}
