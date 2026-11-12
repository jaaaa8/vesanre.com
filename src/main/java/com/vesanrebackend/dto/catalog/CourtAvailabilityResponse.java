package com.vesanrebackend.dto.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CourtAvailabilityResponse(UUID courtId, LocalDate date, String timezone, boolean closed,
                                        int bookingStepMinutes, int minBookingMinutes, int maxBookingMinutes,
                                        List<Slot> slots) {
    public record Slot(LocalTime start, LocalTime end, boolean available, BigDecimal price) {
    }
}
