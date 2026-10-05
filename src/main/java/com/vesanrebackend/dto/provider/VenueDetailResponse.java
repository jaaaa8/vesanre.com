package com.vesanrebackend.dto.provider;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VenueDetailResponse(UUID id, String slug, String name, String description, String addressLine,
                                  String ward, String district, String city, String province, String postalCode,
                                  BigDecimal latitude, BigDecimal longitude, String phone, String status,
                                  List<CourtItem> courts, List<AmenityItem> amenities,
                                  PendingChangeResponse pendingChange, List<ImageResponse> images) {
    public record CourtItem(UUID id, String code, String name, String status) {
    }

    public record AmenityItem(UUID amenityId, String code, String name, String details) {
    }
}
