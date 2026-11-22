package com.vesanrebackend.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProviderCancelRequest(@NotBlank @Size(max = 500) String reason) {
}
