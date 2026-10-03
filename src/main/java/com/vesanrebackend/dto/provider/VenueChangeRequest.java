package com.vesanrebackend.dto.provider;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Important venue fields; null = not sent. For optional fields "" clears the value.
public record VenueChangeRequest(
        @Size(max = 160) String name,
        @Size(max = 255) String addressLine,
        @Size(max = 120) String ward,
        @Size(max = 120) String district,
        @Size(max = 120) String city,
        @Size(max = 120) String province,
        @Size(max = 20) String postalCode,
        @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal longitude) {
}
