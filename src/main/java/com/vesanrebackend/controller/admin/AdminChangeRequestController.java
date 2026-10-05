package com.vesanrebackend.controller.admin;

import com.vesanrebackend.dto.admin.AdminChangeRequestResponse;
import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.admin.RejectProviderRequest;
import com.vesanrebackend.entity.enums.ChangeRequestStatus;
import com.vesanrebackend.entity.enums.ChangeRequestTargetType;
import com.vesanrebackend.service.admin.AdminChangeRequestService;
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
@RequestMapping("/api/admin/change-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChangeRequestController {
    private final AdminChangeRequestService adminChangeRequests;

    public AdminChangeRequestController(AdminChangeRequestService adminChangeRequests) {
        this.adminChangeRequests = adminChangeRequests;
    }

    @GetMapping
    public PageResponse<AdminChangeRequestResponse> list(@RequestParam(defaultValue = "PENDING") ChangeRequestStatus status,
                                                         @RequestParam(required = false) ChangeRequestTargetType targetType,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return adminChangeRequests.list(status, targetType, Paging.of(page, size, "id"));
    }

    @PostMapping("/{id}/approve")
    public AdminChangeRequestResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return adminChangeRequests.approve(UUID.fromString(jwt.getSubject()), id);
    }

    @PostMapping("/{id}/reject")
    public AdminChangeRequestResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                             @Valid @RequestBody RejectProviderRequest request) {
        return adminChangeRequests.reject(UUID.fromString(jwt.getSubject()), id, request.reason());
    }
}
