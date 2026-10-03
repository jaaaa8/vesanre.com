package com.vesanrebackend.controller.provider;

import com.vesanrebackend.dto.provider.CreateVenueRequest;
import com.vesanrebackend.dto.provider.UpdateVenueRequest;
import com.vesanrebackend.dto.provider.VenueAmenityRequest;
import com.vesanrebackend.dto.provider.VenueChangeRequest;
import com.vesanrebackend.dto.provider.VenueDetailResponse;
import com.vesanrebackend.dto.provider.VenueSummaryResponse;
import com.vesanrebackend.service.provider.ProviderVenueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/provider/venues")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderVenueController {
    private final ProviderVenueService venues;

    public ProviderVenueController(ProviderVenueService venues) {
        this.venues = venues;
    }

    @GetMapping
    public List<VenueSummaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return venues.list(userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenueDetailResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateVenueRequest request) {
        return venues.create(userId(jwt), request);
    }

    @GetMapping("/{id}")
    public VenueDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return venues.get(userId(jwt), id);
    }

    @PatchMapping("/{id}")
    public VenueDetailResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @Valid @RequestBody UpdateVenueRequest request) {
        return venues.update(userId(jwt), id, request);
    }

    @PostMapping("/{id}/change-request")
    @ResponseStatus(HttpStatus.CREATED)
    public VenueDetailResponse submitChange(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                            @Valid @RequestBody VenueChangeRequest request) {
        return venues.submitChange(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        venues.delete(userId(jwt), id);
    }

    @PostMapping("/{id}/submit")
    public VenueDetailResponse submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return venues.submit(userId(jwt), id);
    }

    @PutMapping("/{id}/amenities")
    public VenueDetailResponse replaceAmenities(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                @RequestBody List<@NotNull @Valid VenueAmenityRequest> items) {
        return venues.replaceAmenities(userId(jwt), id, items);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
