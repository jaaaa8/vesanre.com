package com.vesanrebackend.dto.booking;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateBookingRequest(@NotNull UUID courtId, @NotNull LocalDate date, @NotNull LocalTime startTime,
                                   @NotNull LocalTime endTime) {
}
