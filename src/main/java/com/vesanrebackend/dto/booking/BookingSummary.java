package com.vesanrebackend.dto.booking;

import com.vesanrebackend.entity.booking.Booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingSummary(UUID id, String bookingCode, String status, String venueName, String courtName,
                             Instant startAt, Instant endAt, String timezone, BigDecimal subtotalAmount,
                             long depositCoins, String currency, Instant createdAt) {
    /** Needs an open session: block, court and venue are lazy. */
    public static BookingSummary of(Booking b) {
        return new BookingSummary(b.getId(), b.getBookingCode(), b.getStatus().name(), b.getVenueNameSnapshot(),
                b.getCourtNameSnapshot(), b.getScheduleBlock().getStartAt(), b.getScheduleBlock().getEndAt(),
                b.getScheduleBlock().getCourt().getVenue().getTimezone(), b.getSubtotalAmount(),
                b.getDepositCoins(), b.getCurrency(), b.getCreatedAt());
    }
}
