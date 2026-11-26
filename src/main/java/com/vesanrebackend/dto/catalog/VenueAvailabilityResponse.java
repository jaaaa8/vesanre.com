package com.vesanrebackend.dto.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record VenueAvailabilityResponse(UUID venueId, LocalDate date, boolean closed, int courtCount,
                                        int depositPercent, List<Slot> slots) {
    public record Slot(LocalTime start, LocalTime end, BigDecimal price, int freeCourts, int freeCourts2h) {
    }
}
