package com.vesanrebackend.controller.admin;

import com.vesanrebackend.dto.admin.AdminProviderResponse;
import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.admin.RejectProviderRequest;
import com.vesanrebackend.entity.enums.ProviderStatus;
import com.vesanrebackend.service.admin.AdminProviderService;
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
@RequestMapping("/api/admin/providers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProviderController {
    private final AdminProviderService adminProviders;

    public AdminProviderController(AdminProviderService adminProviders) {
        this.adminProviders = adminProviders;
    }

    @GetMapping
    public PageResponse<AdminProviderResponse> list(@RequestParam(defaultValue = "PENDING") ProviderStatus status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return adminProviders.list(status, Paging.of(page, size, "userId")); // ProviderProfile id is userId
    }

    @PostMapping("/{userId}/approve")
    public AdminProviderResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        return adminProviders.approve(UUID.fromString(jwt.getSubject()), userId);
    }

    @PostMapping("/{userId}/reject")
    public AdminProviderResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                                        @Valid @RequestBody RejectProviderRequest request) {
        return adminProviders.reject(UUID.fromString(jwt.getSubject()), userId, request.reason());
    }
}
