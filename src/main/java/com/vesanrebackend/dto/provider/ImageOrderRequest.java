package com.vesanrebackend.dto.provider;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ImageOrderRequest(@NotNull UUID id, @Size(max = 255) String altText) {
}
