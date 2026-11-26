package com.vesanrebackend.service.booking;

import com.vesanrebackend.dto.booking.PriceLine;
import com.vesanrebackend.dto.catalog.CourtAvailabilityResponse.Slot;
import com.vesanrebackend.entity.court.CourtOperatingHour;
import com.vesanrebackend.entity.court.CourtPricingRule;
import com.vesanrebackend.service.booking.BookingPricing.Quote;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Loại test: unit thuần (không DB) cho BookingPricing - báo giá, cọc làm tròn xuống, khoảng giờ không hợp lệ.
 */
class BookingPricingTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate DATE = LocalDate.of(2026, 12, 7);

    private static List<Slot> daySlots() {
        CourtOperatingHour h = new CourtOperatingHour();
        h.setIsClosed(false);
        h.setOpensAt(LocalTime.of(6, 0));
        h.setClosesAt(LocalTime.of(22, 0));
        return com.vesanrebackend.service.catalog.AvailabilityCalculator.slots(DATE, ZONE, h, 30,
                List.of(rule(0, 1080, 100000), rule(1080, 1440, 200000)), List.of(),
                DATE.atStartOfDay(ZONE).toInstant());
    }

    private static CourtPricingRule rule(int start, int end, long price) {
        CourtPricingRule r = new CourtPricingRule();
        r.setStartMinute(start);
        r.setEndMinute(end);
        r.setPricePerHour(BigDecimal.valueOf(price));
        return r;
    }

    private static void assertNotBookable(List<Slot> slots, LocalTime start, LocalTime end) {
        assertThatThrownBy(() -> BookingPricing.quote(slots, start, end, 30))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(400);
                    assertThat(e.getReason()).isEqualTo("Requested time is not bookable");
                });
    }

    // Kiểm tra: 17:30-19:00 qua hai rule giá -> subtotal, cọc, đơn giá trung bình và từng dòng giá.
    @Test
    void ninetyMinutesAcrossTwoRules() {
        Quote q = BookingPricing.quote(daySlots(), LocalTime.of(17, 30), LocalTime.of(19, 0), 30);
        assertThat(q.subtotal()).isEqualByComparingTo("250000");
        assertThat(q.depositCoins()).isEqualTo(75);
        assertThat(q.depositAmount()).isEqualByComparingTo("75000");
        assertThat(q.depositPercent()).isEqualTo(30);
        assertThat(q.unitPrice()).isEqualByComparingTo("166667");
        assertThat(q.lines()).extracting(PriceLine::price).map(BigDecimal::intValue)
                .containsExactly(50000, 100000, 100000);
    }

    // Kiểm tra: cọc làm tròn xuống (255500 * 30% = 76650 VND -> 76 coin).
    @Test
    void roundsDepositDown() {
        List<Slot> slots = List.of(new Slot(LocalTime.of(8, 0), LocalTime.of(9, 0), true, new BigDecimal("255500")));
        assertThat(BookingPricing.quote(slots, LocalTime.of(8, 0), LocalTime.of(9, 0), 30).depositCoins()).isEqualTo(76);
    }

    // Kiểm tra: cọc ra 0 coin bị từ chối.
    @Test
    void tinySubtotalRejected() {
        List<Slot> slots = List.of(new Slot(LocalTime.of(8, 0), LocalTime.of(9, 0), true, new BigDecimal("3000")));
        assertThatThrownBy(() -> BookingPricing.quote(slots, LocalTime.of(8, 0), LocalTime.of(9, 0), 30))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(400);
                    assertThat(e.getReason()).isEqualTo("Deposit is too small");
                });
    }

    // Kiểm tra: lệch bước, ngoài giờ, thiếu slot giữa chừng, slot không available -> 400 "not bookable".
    @Test
    void rejectsOffStepOutsideHoursUnpricedAndPast() {
        List<Slot> slots = daySlots();
        assertNotBookable(slots, LocalTime.of(17, 45), LocalTime.of(18, 45));
        assertNotBookable(slots, LocalTime.of(21, 30), LocalTime.of(22, 30));

        List<Slot> gap = new ArrayList<>(slots);
        gap.removeIf(s -> s.start().equals(LocalTime.of(10, 30)));
        assertNotBookable(gap, LocalTime.of(10, 0), LocalTime.of(11, 30));

        List<Slot> taken = new ArrayList<>(slots);
        taken.replaceAll(s -> s.start().equals(LocalTime.of(10, 30))
                ? new Slot(s.start(), s.end(), false, s.price()) : s);
        assertNotBookable(taken, LocalTime.of(10, 0), LocalTime.of(11, 30));
    }

    // Kiểm tra (Review Focus 1): end <= start (kể cả 00:00) là 400, không phải 500.
    @Test
    void rejectsEndNotAfterStart() {
        List<Slot> slots = daySlots();
        assertNotBookable(slots, LocalTime.of(18, 0), LocalTime.of(18, 0));
        assertNotBookable(slots, LocalTime.of(19, 0), LocalTime.of(18, 0));
        assertNotBookable(slots, LocalTime.of(23, 0), LocalTime.MIDNIGHT);
    }

    // Kiểm tra: cọc 0% cho quote hợp lệ với deposit 0 coin.
    @Test
    void zeroPercentGivesZeroDeposit() {
        List<Slot> slots = List.of(
                new Slot(LocalTime.of(8, 0), LocalTime.of(9, 0), true, new BigDecimal("100000")),
                new Slot(LocalTime.of(9, 0), LocalTime.of(10, 0), true, new BigDecimal("100000")));
        Quote q = BookingPricing.quote(slots, LocalTime.of(8, 0), LocalTime.of(10, 0), 0);
        assertThat(q.depositCoins()).isZero();
        assertThat(q.depositAmount().compareTo(BigDecimal.ZERO)).isZero();
        assertThat(q.subtotal()).isEqualByComparingTo("200000");
    }

    // Kiểm tra: % dương nhưng làm tròn xuống 0 coin vẫn bị từ chối.
    @Test
    void positivePercentRoundingToZeroStillFails() {
        List<Slot> slots = List.of(new Slot(LocalTime.of(8, 0), LocalTime.of(9, 0), true, new BigDecimal("1000")));
        assertThatThrownBy(() -> BookingPricing.quote(slots, LocalTime.of(8, 0), LocalTime.of(9, 0), 1))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(400);
                    assertThat(e.getReason()).isEqualTo("Deposit is too small");
                });
    }
}
