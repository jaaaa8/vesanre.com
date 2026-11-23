package com.vesanrebackend.dto.catalog;

import com.vesanrebackend.dto.provider.ImageResponse;
import com.vesanrebackend.dto.provider.PricingRuleRequest;
import com.vesanrebackend.dto.provider.VenueDetailResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicVenueDetailResponse(UUID id, String name, String description, String addressLine, String ward,
                                        String district, String city, String province, BigDecimal latitude,
                                        BigDecimal longitude, String phone, ShopItem shop, List<ImageResponse> images,
                                        List<VenueDetailResponse.AmenityItem> amenities, String sport, int courtCount,
                                        int depositPercent, List<PricingRuleRequest> pricingRules,
                                        BigDecimal fromPrice, BigDecimal ratingAverage, long ratingCount) {
    public record ShopItem(String name, String logoUrl) {
    }
}
