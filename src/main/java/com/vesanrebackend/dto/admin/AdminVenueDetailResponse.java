package com.vesanrebackend.dto.admin;

import com.vesanrebackend.dto.provider.VenueDetailResponse;
import com.vesanrebackend.dto.provider.VenueSetupResponse;

public record AdminVenueDetailResponse(VenueDetailResponse venue, VenueSetupResponse setup) {
}
