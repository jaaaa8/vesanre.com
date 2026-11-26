package com.vesanrebackend.dto.provider;

import java.util.List;

public record VenueSetupResponse(String sportCode, int courtCount, int depositPercent,
                                 List<OperatingHourRequest> operatingHours, List<PricingRuleRequest> pricingRules) {
}
