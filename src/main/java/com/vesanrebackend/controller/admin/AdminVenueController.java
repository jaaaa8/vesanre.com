package com.vesanrebackend.controller.admin;

import com.vesanrebackend.dto.admin.AdminVenueDetailResponse;
import com.vesanrebackend.dto.admin.AdminVenueSummaryResponse;
import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.admin.RejectProviderRequest;
import com.vesanrebackend.dto.provider.VenueDetailResponse;
import com.vesanrebackend.entity.enums.VenueStatus;
import com.vesanrebackend.service.admin.AdminVenueService;
import com.vesanrebackend.util.Paging;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/venues")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVenueController {
    private final AdminVenueService adminVenues;

    public AdminVenueController(AdminVenueService adminVenues) {
        this.adminVenues = adminVenues;
    }

    @GetMapping
    public PageResponse<AdminVenueSummaryResponse> list(@RequestParam(defaultValue = "PENDING_REVIEW") VenueStatus status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return adminVenues.list(status, Paging.of(page, size, "id"));
    }

    @GetMapping("/{id}")
    public AdminVenueDetailResponse get(@PathVariable UUID id) {
        return adminVenues.get(id);
    }

    @PostMapping("/{id}/approve")
    public VenueDetailResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return adminVenues.approve(UUID.fromString(jwt.getSubject()), id);
    }

    @PostMapping("/{id}/reject")
    public VenueDetailResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @Valid @RequestBody RejectProviderRequest request) {
        return adminVenues.reject(UUID.fromString(jwt.getSubject()), id, request.reason());
    }

    @PostMapping("/{id}/suspend")
    public VenueDetailResponse suspend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                       @Valid @RequestBody RejectProviderRequest request) {
        return adminVenues.suspend(UUID.fromString(jwt.getSubject()), id, request.reason());
    }

    @PostMapping("/{id}/reactivate")
    public VenueDetailResponse reactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return adminVenues.reactivate(UUID.fromString(jwt.getSubject()), id);
    }
}
