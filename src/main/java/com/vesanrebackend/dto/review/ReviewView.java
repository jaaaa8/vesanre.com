package com.vesanrebackend.dto.review;

import com.vesanrebackend.entity.booking.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewView(UUID id, int rating, String comment, String customerName, String status,
                         String providerReply, Instant repliedAt, Instant createdAt) {
    /** Needs an open session (customer is LAZY). */
    public static ReviewView of(Review r) {
        return new ReviewView(r.getId(), r.getRating(), r.getComment(), r.getCustomer().getDisplayName(),
                r.getStatus().name(), r.getProviderReply(), r.getRepliedAt(), r.getCreatedAt());
    }
}
