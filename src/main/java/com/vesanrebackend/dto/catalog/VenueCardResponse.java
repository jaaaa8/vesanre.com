package com.vesanrebackend.dto.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VenueCardResponse(UUID id, String name, String addressLine, String district, String city,
                                BigDecimal latitude, BigDecimal longitude, String coverImageUrl,
                                List<String> sports, BigDecimal fromPrice) {
}
