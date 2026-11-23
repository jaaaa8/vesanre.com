package com.vesanrebackend.service.booking;

import com.vesanrebackend.dto.booking.PriceLine;
import com.vesanrebackend.dto.catalog.CourtAvailabilityResponse.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Pure pricing for a requested time range over one day's slots; 1 coin = 1.000 VND. */
public final class BookingPricing {
    public static final int DEFAULT_DEPOSIT_PERCENT = 30;
    private static final Logger log = LoggerFactory.getLogger(BookingPricing.class);
    private static final BigDecimal VND_PER_COIN = BigDecimal.valueOf(1000);

    private BookingPricing() {
    }

    public record Quote(BigDecimal subtotal, BigDecimal unitPrice, long depositCoins, BigDecimal depositAmount,
                        int depositPercent, List<PriceLine> lines) {
    }

    public static Quote quote(List<Slot> slots, LocalTime start, LocalTime end, int depositPercent) {
        if (!end.isAfter(start)) { // also covers end = 00:00 (midnight is not after any start)
            throw notBookable();
        }
        List<Slot> chosen = new ArrayList<>();
        for (Slot s : slots) {
            if (!s.start().isBefore(start) && !s.end().isAfter(end)) {
                chosen.add(s);
            }
        }
        // Contiguous, all available, exactly [start, end).
        boolean ok = !chosen.isEmpty() && chosen.get(0).start().equals(start)
                && chosen.get(chosen.size() - 1).end().equals(end);
        for (int i = 0; ok && i < chosen.size(); i++) {
            ok = chosen.get(i).available() && (i == 0 || chosen.get(i - 1).end().equals(chosen.get(i).start()));
        }
        if (!ok) {
            throw notBookable();
        }
        BigDecimal subtotal = chosen.stream().map(Slot::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        long coins = subtotal.multiply(BigDecimal.valueOf(depositPercent))
                .divide(BigDecimal.valueOf(100).multiply(VND_PER_COIN), 0, RoundingMode.FLOOR).longValueExact();
        if (depositPercent > 0 && coins == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deposit is too small");
        }
        long minutes = (end.toSecondOfDay() - start.toSecondOfDay()) / 60L;
        BigDecimal unit = subtotal.multiply(BigDecimal.valueOf(60)).divide(BigDecimal.valueOf(minutes), 0, RoundingMode.HALF_UP);
        return new Quote(subtotal, unit, coins, VND_PER_COIN.multiply(BigDecimal.valueOf(coins)), depositPercent,
                chosen.stream().map(s -> new PriceLine(s.start(), s.end(), s.price())).toList());
    }

    /** Integer 1-100 under "depositPercent"; anything else falls back to the default. */
    public static int depositPercent(String policyJson, ObjectMapper json) {
        if (policyJson != null) {
            try {
                JsonNode n = json.readTree(policyJson).get("depositPercent");
                if (n != null && n.isIntegralNumber() && n.canConvertToInt() && n.intValue() >= 1 && n.intValue() <= 100) {
                    return n.intValue();
                }
            } catch (RuntimeException e) {
                // fall through to the default
            }
        }
        log.warn("Invalid or missing depositPercent in cancellation policy, using {}", DEFAULT_DEPOSIT_PERCENT);
        return DEFAULT_DEPOSIT_PERCENT;
    }

    private static ResponseStatusException notBookable() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested time is not bookable");
    }
}
