package com.vesanrebackend.dto.admin;

import com.vesanrebackend.dto.provider.CourtDetailResponse;
import com.vesanrebackend.dto.provider.VenueDetailResponse;

import java.util.List;

public record AdminVenueDetailResponse(VenueDetailResponse venue, List<CourtDetailResponse> courts) {
}
