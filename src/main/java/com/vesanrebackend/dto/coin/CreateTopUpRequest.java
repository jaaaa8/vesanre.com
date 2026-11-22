package com.vesanrebackend.dto.coin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateTopUpRequest(@NotNull @Min(10) @Max(5000) Long coins) {
}
