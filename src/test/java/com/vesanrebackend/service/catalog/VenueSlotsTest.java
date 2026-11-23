package com.vesanrebackend.service.catalog;

import com.vesanrebackend.dto.catalog.CourtAvailabilityResponse.Slot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Loại test: unit thuần cho VenueSlots.merge (không DB). */
class VenueSlotsTest {
    private static Slot slot(int hour, boolean available) {
        return new Slot(LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0), available, BigDecimal.valueOf(100000));
    }

    private static List<Slot> court(boolean... free) {
        return java.util.stream.IntStream.range(0, free.length).mapToObj(i -> slot(7 + i, free[i])).toList();
    }

    // Kiểm tra: freeCourts là số sân rảnh ở từng mốc.
    @Test
    void countsFreeCourtsPerHour() {
        var out = VenueSlots.merge(List.of(court(true, true), court(false, true), court(true, true)));
        assertThat(out.get(0).freeCourts()).isEqualTo(2);
        assertThat(out.get(1).freeCourts()).isEqualTo(3);
    }

    // Kiểm tra: 2 giờ liền phải cùng một sân rảnh cả hai mốc.
    @Test
    void twoHourNeedsSameCourt() {
        var out = VenueSlots.merge(List.of(court(true, false), court(false, true)));
        assertThat(out.get(0).freeCourts()).isEqualTo(1);
        assertThat(out.get(1).freeCourts()).isEqualTo(1);
        assertThat(out.get(0).freeCourts2h()).isZero();
    }

    // Kiểm tra: mốc cuối trước giờ đóng cửa không có 2 giờ.
    @Test
    void lastSlotHasNoTwoHour() {
        var out = VenueSlots.merge(List.of(court(true, true)));
        assertThat(out.get(0).freeCourts2h()).isEqualTo(1);
        assertThat(out.get(1).freeCourts2h()).isZero();
    }

    // Kiểm tra: không có sân thì không có slot.
    @Test
    void noCourtsGivesEmpty() {
        assertThat(VenueSlots.merge(List.of())).isEmpty();
    }
}
