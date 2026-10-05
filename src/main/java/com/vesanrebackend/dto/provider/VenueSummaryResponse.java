package com.vesanrebackend.dto.provider;

import java.util.UUID;

public record VenueSummaryResponse(UUID id, String slug, String name, String district, String city, String status) {
}
