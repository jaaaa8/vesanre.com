package com.vesanrebackend.dto.provider;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Lengths match the venues columns; latitude/longitude must be both set or both null (checked in the service).
public record CreateVenueRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 255) String addressLine,
        @Size(max = 120) String ward,
        @NotBlank @Size(max = 120) String district,
        @NotBlank @Size(max = 120) String city,
        @Size(max = 120) String province,
        @Size(max = 20) String postalCode,
        @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal longitude,
        @Size(max = 32) String phone) {
}
