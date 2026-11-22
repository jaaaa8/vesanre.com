package com.vesanrebackend.dto.booking;

import com.vesanrebackend.dto.review.ReviewView;
import com.vesanrebackend.entity.booking.Booking;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingDetail(UUID id, String bookingCode, String status, String venueName, String courtName,
                            Instant startAt, Instant endAt, String timezone, BigDecimal subtotalAmount,
                            long depositCoins, String currency, Instant createdAt, UUID venueId, UUID courtId,
                            String shopName, List<PriceLine> priceBreakdown, int depositPercent,
                            String cancellationSource, String cancellationReason, Instant confirmedAt,
                            Instant cancelledAt, Instant completedAt, CustomerContact customer,
                            ReviewView review) {
    /** Needs an open session; customer contact is only filled for the provider view. */
    public static BookingDetail of(Booking b, ObjectMapper json, boolean withCustomer) {
        BookingSummary s = BookingSummary.of(b);
        var court = b.getScheduleBlock().getCourt();
        int percent = json.readTree(b.getCancellationPolicySnapshot()).get("depositPercent").intValue();
        List<PriceLine> lines = json.readValue(b.getPriceBreakdownSnapshot(), new TypeReference<List<PriceLine>>() { });
        return new BookingDetail(s.id(), s.bookingCode(), s.status(), s.venueName(), s.courtName(), s.startAt(),
                s.endAt(), s.timezone(), s.subtotalAmount(), s.depositCoins(), s.currency(), s.createdAt(),
                court.getVenue().getId(), court.getId(), b.getShopNameSnapshot(), lines, percent,
                b.getCancellationSource() == null ? null : b.getCancellationSource().name(),
                b.getCancellationReason(), b.getConfirmedAt(), b.getCancelledAt(), b.getCompletedAt(),
                withCustomer ? new CustomerContact(b.getCustomer().getDisplayName(), b.getCustomer().getPhone()) : null,
                null);
    }

    public BookingDetail withReview(ReviewView review) {
        return new BookingDetail(id, bookingCode, status, venueName, courtName, startAt, endAt, timezone,
                subtotalAmount, depositCoins, currency, createdAt, venueId, courtId, shopName, priceBreakdown,
                depositPercent, cancellationSource, cancellationReason, confirmedAt, cancelledAt, completedAt,
                customer, review);
    }
}
