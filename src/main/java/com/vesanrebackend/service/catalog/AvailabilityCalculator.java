package com.vesanrebackend.service.catalog;

import com.vesanrebackend.dto.catalog.CourtAvailabilityResponse.Slot;
import com.vesanrebackend.entity.court.CourtOperatingHour;
import com.vesanrebackend.entity.court.CourtPricingRule;
import com.vesanrebackend.entity.court.CourtScheduleBlock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Pure slot calculation for one court and one date; rules/blocks are pre-filtered by the caller. */
public final class AvailabilityCalculator {
    private AvailabilityCalculator() {
    }

    public static List<Slot> slots(LocalDate date, ZoneId zone, CourtOperatingHour hours, int stepMinutes,
                                   List<CourtPricingRule> rules, List<CourtScheduleBlock> blocks, Instant now) {
        if (hours == null || hours.getIsClosed()) {
            return List.of();
        }
        List<Slot> slots = new ArrayList<>();
        int open = hours.getOpensAt().toSecondOfDay() / 60;
        int close = hours.getClosesAt().toSecondOfDay() / 60;
        for (int t = open; t + stepMinutes <= close; t += stepMinutes) {
            int end = t + stepMinutes;
            int covered = 0;
            BigDecimal price = BigDecimal.ZERO;
            for (CourtPricingRule r : rules) {
                int overlap = Math.min(end, r.getEndMinute()) - Math.max(t, r.getStartMinute());
                if (overlap > 0) {
                    covered += overlap;
                    price = price.add(r.getPricePerHour().multiply(BigDecimal.valueOf(overlap)));
                }
            }
            if (covered < stepMinutes) {
                continue; // rules never overlap (DB exclusion), so covered == step means fully priced
            }
            // atTime(...).atZone: correct on DST days too (Sprint 3 reuses this)
            Instant s = date.atTime(LocalTime.ofSecondOfDay(t * 60L)).atZone(zone).toInstant();
            Instant e = date.atTime(LocalTime.ofSecondOfDay(end * 60L)).atZone(zone).toInstant();
            boolean busy = blocks.stream().anyMatch(b -> b.getStartAt().isBefore(e) && s.isBefore(b.getEndAt()));
            slots.add(new Slot(LocalTime.ofSecondOfDay(t * 60L), LocalTime.ofSecondOfDay(end * 60L),
                    !busy && s.isAfter(now), price.divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP)));
        }
        return slots;
    }
}
