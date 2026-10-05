package com.vesanrebackend.dto.provider;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record OperatingHourRequest(@NotNull @Min(0) @Max(6) Integer weekday, LocalTime opensAt, LocalTime closesAt,
                                   @NotNull Boolean closed) {
}
