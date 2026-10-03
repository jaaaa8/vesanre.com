package com.vesanrebackend.dto.catalog;

import com.vesanrebackend.entity.enums.AmenityScope;

import java.util.UUID;

public record AmenityResponse(UUID id, String code, String name, AmenityScope scope) {
}
