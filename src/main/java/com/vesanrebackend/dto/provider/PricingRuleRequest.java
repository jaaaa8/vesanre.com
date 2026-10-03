package com.vesanrebackend.dto.provider;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PricingRuleRequest(@NotNull @Min(0) @Max(6) Integer weekday, @NotNull @Min(0) @Max(1440) Integer startMinute,
                                 @NotNull @Min(0) @Max(1440) Integer endMinute,
                                 @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal pricePerHour) {
}
