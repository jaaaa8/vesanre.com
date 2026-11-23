package com.vesanrebackend.dto.provider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VenueSetupRequest(@NotBlank String sportCode, @Min(1) @Max(50) int courtCount,
                                @Min(0) @Max(100) int depositPercent,
                                @NotNull @Valid List<OperatingHourRequest> operatingHours,
                                @NotNull @Valid List<PricingRuleRequest> pricingRules) {
}
