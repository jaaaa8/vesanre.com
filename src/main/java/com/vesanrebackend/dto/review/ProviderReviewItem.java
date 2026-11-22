package com.vesanrebackend.dto.review;

import com.vesanrebackend.entity.booking.Review;

public record ProviderReviewItem(ReviewView review, String venueName, String bookingCode) {
    /** Needs an open session (booking chain is LAZY). */
    public static ProviderReviewItem of(Review r) {
        return new ProviderReviewItem(ReviewView.of(r),
                r.getBooking().getScheduleBlock().getCourt().getVenue().getName(), r.getBooking().getBookingCode());
    }
}
