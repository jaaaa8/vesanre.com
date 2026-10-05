package com.vesanrebackend.dto.provider;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CourtSportRequest(@NotNull UUID sportId, @NotNull Boolean primary) {
}
