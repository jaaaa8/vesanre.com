package com.vesanrebackend.controller.provider;

import com.vesanrebackend.dto.provider.CourtDetailResponse;
import com.vesanrebackend.dto.provider.CourtSportRequest;
import com.vesanrebackend.dto.provider.CreateCourtRequest;
import com.vesanrebackend.dto.provider.OperatingHourRequest;
import com.vesanrebackend.dto.provider.PricingRuleRequest;
import com.vesanrebackend.dto.provider.UpdateCourtRequest;
import com.vesanrebackend.dto.provider.VenueAmenityRequest;
import com.vesanrebackend.service.provider.ProviderCourtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/provider")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderCourtController {
    private final ProviderCourtService courts;

    public ProviderCourtController(ProviderCourtService courts) {
        this.courts = courts;
    }

    @PostMapping("/venues/{venueId}/courts")
    @ResponseStatus(HttpStatus.CREATED)
    public CourtDetailResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
                                      @Valid @RequestBody CreateCourtRequest request) {
        return courts.create(userId(jwt), venueId, request);
    }

    @GetMapping("/courts/{id}")
    public CourtDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return courts.get(userId(jwt), id);
    }

    @PatchMapping("/courts/{id}")
    public CourtDetailResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @Valid @RequestBody UpdateCourtRequest request) {
        return courts.update(userId(jwt), id, request);
    }

    @PutMapping("/courts/{id}/sports")
    public CourtDetailResponse replaceSports(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                             @RequestBody List<@NotNull @Valid CourtSportRequest> items) {
        return courts.replaceSports(userId(jwt), id, items);
    }

    @PutMapping("/courts/{id}/amenities")
    public CourtDetailResponse replaceAmenities(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                @RequestBody List<@NotNull @Valid VenueAmenityRequest> items) {
        return courts.replaceAmenities(userId(jwt), id, items);
    }

    @PutMapping("/courts/{id}/operating-hours")
    public CourtDetailResponse replaceOperatingHours(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                     @RequestBody List<@NotNull @Valid OperatingHourRequest> items) {
        return courts.replaceOperatingHours(userId(jwt), id, items);
    }

    @PutMapping("/courts/{id}/pricing-rules")
    public CourtDetailResponse replacePricingRules(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                   @RequestBody List<@NotNull @Valid PricingRuleRequest> items) {
        return courts.replacePricingRules(userId(jwt), id, items);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
