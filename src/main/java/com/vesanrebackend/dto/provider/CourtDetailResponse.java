package com.vesanrebackend.dto.provider;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CourtDetailResponse(UUID id, UUID venueId, String code, String name, String description,
                                  Integer capacity, Integer bookingStepMinutes, Integer minBookingMinutes,
                                  Integer maxBookingMinutes, String status, List<SportItem> sports,
                                  List<AmenityItem> amenities, List<OperatingHourItem> operatingHours,
                                  List<PricingRuleItem> pricingRules, List<ImageResponse> images) {
    public record SportItem(UUID sportId, String code, String name, boolean primary) {
    }

    public record AmenityItem(UUID amenityId, String code, String name, String details) {
    }

    public record OperatingHourItem(int weekday, LocalTime opensAt, LocalTime closesAt, boolean closed) {
    }

    public record PricingRuleItem(UUID id, int weekday, int startMinute, int endMinute, BigDecimal pricePerHour,
                                  String currency) {
    }
}
