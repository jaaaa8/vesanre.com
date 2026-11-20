package com.vesanrebackend.controller.provider;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.review.ProviderReviewItem;
import com.vesanrebackend.dto.review.ReplyRequest;
import com.vesanrebackend.dto.review.ReviewView;
import com.vesanrebackend.service.review.ReviewService;
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
@RequestMapping("/api/provider/reviews")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderReviewController {
    private final ReviewService reviews;

    public ProviderReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public PageResponse<ProviderReviewItem> list(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return reviews.providerList(UUID.fromString(jwt.getSubject()), page, size);
    }

    @PostMapping("/{id}/reply")
    public ReviewView reply(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                            @Valid @RequestBody ReplyRequest req) {
        return reviews.reply(UUID.fromString(jwt.getSubject()), id, req.reply());
    }
}
