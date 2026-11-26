package com.vesanrebackend.dto.catalog;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Holder of the per-court hourly Slot used by AvailabilityCalculator and BookingPricing. */
public final class CourtAvailabilityResponse {
    private CourtAvailabilityResponse() {
    }

    public record Slot(LocalTime start, LocalTime end, boolean available, BigDecimal price) {
    }
}
