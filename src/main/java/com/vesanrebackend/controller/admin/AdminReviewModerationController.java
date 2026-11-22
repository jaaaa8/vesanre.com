package com.vesanrebackend.controller.admin;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.admin.ReasonRequest;
import com.vesanrebackend.dto.review.ProviderReviewItem;
import com.vesanrebackend.dto.review.ReviewView;
import com.vesanrebackend.entity.enums.ReviewStatus;
import com.vesanrebackend.service.admin.AdminReviewModerationService;
import com.vesanrebackend.util.Paging;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewModerationController {
    private final AdminReviewModerationService moderation;

    public AdminReviewModerationController(AdminReviewModerationService moderation) {
        this.moderation = moderation;
    }

    @GetMapping
    public PageResponse<ProviderReviewItem> list(@RequestParam(required = false) ReviewStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return moderation.list(status, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @PostMapping("/{id}/hide")
    public ReviewView hide(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                           @Valid @RequestBody ReasonRequest req) {
        return moderation.hide(UUID.fromString(jwt.getSubject()), id, req.reason());
    }

    @PostMapping("/{id}/unhide")
    public ReviewView unhide(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return moderation.unhide(UUID.fromString(jwt.getSubject()), id);
    }
}
