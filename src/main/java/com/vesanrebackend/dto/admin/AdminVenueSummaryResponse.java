package com.vesanrebackend.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminVenueSummaryResponse(UUID id, String name, UUID shopId, String shopName, String addressLine,
                                        String district, String city, String status, Instant createdAt) {
}
