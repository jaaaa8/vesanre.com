package com.vesanrebackend.dto.report;

import java.math.BigDecimal;

/** month "YYYY-MM"; changePercent null when the previous month had no bookings. */
public record MonthlyBookings(String month, long bookings, long changeFromPrevious, BigDecimal changePercent) {
}
