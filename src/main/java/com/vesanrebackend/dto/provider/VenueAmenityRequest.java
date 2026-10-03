package com.vesanrebackend.dto.provider;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VenueAmenityRequest(@NotNull UUID amenityId, @Size(max = 255) String details) {
}
