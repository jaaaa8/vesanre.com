package com.vesanrebackend.dto.provider;

import com.vesanrebackend.entity.enums.CourtStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// PATCH body: null = not sent; "" clears description. Unknown status values fail JSON parsing (400).
public record UpdateCourtRequest(
        @Size(max = 50) String code,
        @Size(max = 120) String name,
        @Size(max = 5000) String description,
        @Positive Integer capacity,
        @Positive Integer bookingStepMinutes,
        @Positive Integer minBookingMinutes,
        @Positive Integer maxBookingMinutes,
        CourtStatus status) {
}
