package com.vesanrebackend.service.catalog;

import com.vesanrebackend.dto.catalog.CourtAvailabilityResponse.Slot;
import com.vesanrebackend.entity.court.CourtOperatingHour;
import com.vesanrebackend.entity.court.CourtPricingRule;
import com.vesanrebackend.entity.court.CourtScheduleBlock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loại test: unit thuần (không DB) cho AvailabilityCalculator.slots - sinh slot, giá, block, giờ đã qua.
 */
class AvailabilityCalculatorTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate DATE = LocalDate.of(2026, 12, 7); // Monday
    private static final Instant NOW = Instant.parse("2026-12-01T00:00:00Z");

    // Kiểm tra: ngày đóng cửa hoặc không có dòng giờ mở cửa thì không có slot.
    @Test
    void closedDayHasNoSlots() {
        CourtOperatingHour closed = hours("06:00", "08:00");
        closed.setIsClosed(true);
        assertThat(slots(closed, 60, List.of(rule(0, 1440, 100000)), List.of(), NOW)).isEmpty();
        assertThat(slots(null, 60, List.of(rule(0, 1440, 100000)), List.of(), NOW)).isEmpty();
    }

    // Kiểm tra: slot dừng trước giờ đóng cửa, phần lẻ cuối bị bỏ (06:00-08:30 vẫn 2 slot).
    @Test
    void slotsStopBeforeClosingAndDropPartialTail() {
        for (String closes : new String[]{"08:00", "08:30"}) {
            List<Slot> slots = slots(hours("06:00", closes), 60, List.of(rule(0, 1440, 100000)), List.of(), NOW);
            assertThat(slots).extracting(Slot::start).containsExactly(LocalTime.of(6, 0), LocalTime.of(7, 0));
            assertThat(slots).extracting(Slot::end).containsExactly(LocalTime.of(7, 0), LocalTime.of(8, 0));
            assertThat(slots).allMatch(Slot::available);
            assertThat(slots).allSatisfy(s -> assertThat(s.price()).isEqualByComparingTo("100000"));
        }
    }

    // Kiểm tra: block chồng lấn làm slot unavailable; block chạm mép [) thì không ảnh hưởng.
    @Test
    void blockMakesOverlappingSlotsUnavailable() {
        List<CourtPricingRule> rules = List.of(rule(0, 1440, 100000));
        List<Slot> overlapped = slots(hours("06:00", "08:00"), 60, rules,
                List.of(block(DATE.atTime(6, 30), DATE.atTime(7, 30))), NOW);
        assertThat(overlapped).extracting(Slot::available).containsExactly(false, false);

        List<Slot> touching = slots(hours("06:00", "08:00"), 60, rules,
                List.of(block(DATE.atTime(8, 0), DATE.atTime(9, 0))), NOW);
        assertThat(touching).extracting(Slot::available).containsExactly(true, true);
    }

    // Kiểm tra: slot nằm trên 2 rule thì giá = tổng theo phút (50.000 + 100.000).
    @Test
    void priceSumsMinutesAcrossRules() {
        List<Slot> slots = slots(hours("06:00", "07:00"), 60,
                List.of(rule(360, 390, 100000), rule(390, 1440, 200000)), List.of(), NOW);
        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).price()).isEqualByComparingTo("150000");
    }

    // Kiểm tra: slot có phút không được rule nào phủ thì bị bỏ.
    @Test
    void slotWithUnpricedMinutesIsDropped() {
        List<Slot> slots = slots(hours("06:00", "08:00"), 60, List.of(rule(360, 420, 100000)), List.of(), NOW);
        assertThat(slots).extracting(Slot::start).containsExactly(LocalTime.of(6, 0));
    }

    // Kiểm tra: hôm nay, slot có start <= now thì unavailable; slot sau đó vẫn available.
    @Test
    void pastSlotsTodayAreUnavailable() {
        Instant now = DATE.atTime(7, 30).atZone(ZONE).toInstant();
        List<Slot> slots = slots(hours("06:00", "09:00"), 60, List.of(rule(0, 1440, 100000)), List.of(), now);
        assertThat(slots).extracting(Slot::available).containsExactly(false, false, true);
    }

    // Kiểm tra: block bắt đầu từ hôm trước kéo qua nửa đêm làm slot đầu ngày unavailable.
    @Test
    void blockFromPreviousDayCoversEarlySlot() {
        List<Slot> slots = slots(hours("06:00", "08:00"), 60, List.of(rule(0, 1440, 100000)),
                List.of(block(DATE.minusDays(1).atTime(23, 30), DATE.atTime(6, 30))), NOW);
        assertThat(slots).extracting(Slot::available).containsExactly(false, true);
    }

    private static List<Slot> slots(CourtOperatingHour hours, int step, List<CourtPricingRule> rules,
                                    List<CourtScheduleBlock> blocks, Instant now) {
        return AvailabilityCalculator.slots(DATE, ZONE, hours, step, rules, blocks, now);
    }

    private static CourtOperatingHour hours(String opens, String closes) {
        CourtOperatingHour h = new CourtOperatingHour();
        h.setIsClosed(false);
        h.setOpensAt(LocalTime.parse(opens));
        h.setClosesAt(LocalTime.parse(closes));
        return h;
    }

    private static CourtPricingRule rule(int start, int end, long price) {
        CourtPricingRule r = new CourtPricingRule();
        r.setStartMinute(start);
        r.setEndMinute(end);
        r.setPricePerHour(BigDecimal.valueOf(price));
        return r;
    }

    private static CourtScheduleBlock block(LocalDateTime start, LocalDateTime end) {
        CourtScheduleBlock b = new CourtScheduleBlock();
        b.setStartAt(start.atZone(ZONE).toInstant());
        b.setEndAt(end.atZone(ZONE).toInstant());
        return b;
    }
}
