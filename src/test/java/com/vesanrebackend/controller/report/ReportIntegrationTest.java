package com.vesanrebackend.controller.report;

import com.vesanrebackend.BookingApiTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loại test: integration HTTP thật + PostgreSQL (kế thừa BookingApiTestSupport).
 * API: /api/provider/reports/monthly-bookings, /api/admin/reports/top-shops, /api/admin/reports/finance.
 */
class ReportIntegrationTest extends BookingApiTestSupport {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Moves the booking's block to vnStart (VN wall time) for 1h; bookings on one court need distinct times. */
    private void moveTo(UUID bookingId, LocalDateTime vnStart) {
        var start = vnStart.atZone(VN).toInstant().atOffset(ZoneOffset.UTC);
        jdbc.update("UPDATE sporthub.court_schedule_blocks SET start_at = ?, end_at = ? WHERE id = "
                + "(SELECT schedule_block_id FROM sporthub.bookings WHERE id = ?)", start, start.plusHours(1), bookingId);
    }

    private List<Map> monthly(String token, int months) {
        return getList("/api/provider/reports/monthly-bookings?months=" + months, token);
    }

    // Kiểm tra (Review Focus 4, 5): 23:30 ngày cuối tháng VN thuộc tháng đó; CANCELLED không đếm; tháng trống = 0.
    @Test
    void monthlyCountsByVietnamMonth() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        YearMonth now = YearMonth.now(VN);
        YearMonth prev = now.minusMonths(1);
        UUID lastNightPrev = confirmedBooking(c, bc);
        UUID firstNightNow = confirmedBooking(c, bc);
        UUID cancelled = confirmedBooking(c, bc);
        assertThat(post(client(), "/api/bookings/" + cancelled + "/cancel", Map.of(), c.token())
                .getStatusCode().value()).isEqualTo(200);
        moveTo(lastNightPrev, prev.atEndOfMonth().atTime(23, 30)); // = 16:30 UTC ngày cuối tháng
        moveTo(firstNightNow, now.atDay(1).atTime(0, 30));
        moveTo(cancelled, now.atDay(1).atTime(2, 0));
        List<Map> rows = monthly(bc.provider().token(), 3);
        assertThat(rows).extracting(r -> r.get("month"))
                .containsExactly(now.minusMonths(2).toString(), prev.toString(), now.toString());
        assertThat(rows).extracting(r -> r.get("bookings")).containsExactly(0, 1, 1);
        assertThat(rows.get(0).get("changePercent")).isNull();
        assertThat(rows.get(1).get("changeFromPrevious")).isEqualTo(1);
        assertThat(rows.get(1).get("changePercent")).isNull(); // tháng trước 0
        assertThat(rows.get(2).get("changeFromPrevious")).isEqualTo(0);
        assertThat(new BigDecimal(rows.get(2).get("changePercent").toString())).isEqualByComparingTo("0");
    }

    private void bookInto(BookableCourt bc, Customer c, YearMonth m, int count) {
        for (int i = 0; i < count; i++) {
            moveTo(confirmedBooking(c, bc), m.atDay(1 + i).atTime(10, 0));
        }
    }

    // Kiểm tra: changePercent 1 chữ số HALF_UP, âm khi giảm.
    @Test
    void percentChange() {
        YearMonth now = YearMonth.now(VN);
        YearMonth prev = now.minusMonths(1);
        Customer c = customer();
        BookableCourt a = bookableCourt();
        BookableCourt b = bookableCourt();
        bookInto(a, c, prev, 2);
        bookInto(a, c, now, 3);
        bookInto(b, c, prev, 3);
        bookInto(b, c, now, 1);
        Map last = monthly(a.provider().token(), 2).get(1);
        assertThat(last.get("changeFromPrevious")).isEqualTo(1);
        assertThat(new BigDecimal(last.get("changePercent").toString())).isEqualByComparingTo("50.0");
        last = monthly(b.provider().token(), 2).get(1);
        assertThat(last.get("changeFromPrevious")).isEqualTo(-2);
        assertThat(new BigDecimal(last.get("changePercent").toString())).isEqualByComparingTo("-66.7");
    }

    // Kiểm tra (Review Focus 3): months ngoài 1–24 -> 400; mặc định 6 dòng.
    @Test
    void monthsOutOfRangeRejected() {
        String t = bookableCourt().provider().token();
        assertThat(get(client(), "/api/provider/reports/monthly-bookings?months=0", t).getStatusCode().value())
                .isEqualTo(400);
        assertThat(get(client(), "/api/provider/reports/monthly-bookings?months=25", t).getStatusCode().value())
                .isEqualTo(400);
        assertThat(getList("/api/provider/reports/monthly-bookings", t)).hasSize(6);
    }

    @Test
    void customerForbidden() {
        assertThat(get(client(), "/api/provider/reports/monthly-bookings", customer().token()).getStatusCode().value())
                .isEqualTo(403);
    }
}
