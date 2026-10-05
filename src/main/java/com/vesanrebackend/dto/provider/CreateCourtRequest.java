package com.vesanrebackend.dto.provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Lengths match the courts columns; min/max/step consistency is checked in the service.
public record CreateCourtRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 5000) String description,
        @NotNull @Positive Integer capacity,
        @NotNull @Positive Integer bookingStepMinutes,
        @NotNull @Positive Integer minBookingMinutes,
        @NotNull @Positive Integer maxBookingMinutes) {
}
