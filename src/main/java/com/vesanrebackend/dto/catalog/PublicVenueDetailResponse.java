package com.vesanrebackend.dto.catalog;

import com.vesanrebackend.dto.provider.CourtDetailResponse;
import com.vesanrebackend.dto.provider.ImageResponse;
import com.vesanrebackend.dto.provider.VenueDetailResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicVenueDetailResponse(UUID id, String name, String description, String addressLine, String ward,
                                        String district, String city, String province, BigDecimal latitude,
                                        BigDecimal longitude, String phone, ShopItem shop, List<ImageResponse> images,
                                        List<VenueDetailResponse.AmenityItem> amenities, List<CourtItem> courts,
                                        BigDecimal fromPrice, BigDecimal ratingAverage, long ratingCount) {
    public record ShopItem(String name, String logoUrl) {
    }

    public record CourtItem(UUID id, String code, String name, String description, Integer capacity,
                            Integer bookingStepMinutes, Integer minBookingMinutes, Integer maxBookingMinutes,
                            List<CourtDetailResponse.SportItem> sports, List<CourtDetailResponse.AmenityItem> amenities,
                            List<ImageResponse> images, List<CourtDetailResponse.OperatingHourItem> operatingHours,
                            List<CourtDetailResponse.PricingRuleItem> pricingRules) {
    }
}
