package com.vesanrebackend.service.admin;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.review.ProviderReviewItem;
import com.vesanrebackend.dto.review.ReviewView;
import com.vesanrebackend.entity.booking.Review;
import com.vesanrebackend.entity.enums.ReviewStatus;
import com.vesanrebackend.repository.ReviewRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/** Q19: hiding is admin-only and silent (no notification to the customer). */
@Service
public class AdminReviewModerationService {
    private static final String ENTITY_TYPE = "REVIEW";

    private final ReviewRepository reviews;
    private final AdminAudit audit;

    public AdminReviewModerationService(ReviewRepository reviews, AdminAudit audit) {
        this.reviews = reviews;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProviderReviewItem> list(ReviewStatus status, Pageable pageable) {
        return PageResponse.of((status == null ? reviews.findAll(pageable) : reviews.findByStatus(status, pageable))
                .map(ProviderReviewItem::of));
    }

    @Transactional
    public ReviewView hide(UUID adminId, UUID reviewId, String reason) {
        Review r = transition(reviewId, ReviewStatus.PUBLISHED, ReviewStatus.HIDDEN);
        audit.record(adminId, "REVIEW_HIDDEN", ENTITY_TYPE, reviewId, Map.of("status", "PUBLISHED"),
                Map.of("status", "HIDDEN", "reason", reason));
        return ReviewView.of(r);
    }

    @Transactional
    public ReviewView unhide(UUID adminId, UUID reviewId) {
        Review r = transition(reviewId, ReviewStatus.HIDDEN, ReviewStatus.PUBLISHED);
        audit.record(adminId, "REVIEW_UNHIDDEN", ENTITY_TYPE, reviewId, Map.of("status", "HIDDEN"),
                Map.of("status", "PUBLISHED"));
        return ReviewView.of(r);
    }

    // The row lock makes a concurrent second admin wait, then see the new status and get 409.
    private Review transition(UUID reviewId, ReviewStatus from, ReviewStatus to) {
        Review r = reviews.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (r.getStatus() != from) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review is not " + from);
        }
        r.setStatus(to);
        return reviews.saveAndFlush(r);
    }
}
