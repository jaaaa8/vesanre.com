package com.vesanrebackend.controller.report;

import com.vesanrebackend.BookingApiTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loại test: integration HTTP thật + PostgreSQL (kế thừa BookingApiTestSupport).
 * API: /api/provider/reports/monthly-bookings, /api/admin/reports/top-shops, /api/admin/reports/finance.
 */
class ReportIntegrationTest extends BookingApiTestSupport {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Random RANDOM = new Random();

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

    // Shared, never-cleaned DB: aggregate tests use a random past month and compare only their own shops / deltas.
    private static YearMonth randomPastMonth() {
        return YearMonth.of(2000 + RANDOM.nextInt(20), 1 + RANDOM.nextInt(12));
    }

    // Kiểm tra (Review Focus 3, 5): top shop giảm dần, CANCELLED không đếm; limit/month sai -> 400.
    @Test
    void topShopsDescending() {
        YearMonth m = randomPastMonth();
        Customer c = customer();
        BookableCourt a = bookableCourt();
        BookableCourt b = bookableCourt();
        bookInto(a, c, m, 2);
        bookInto(b, c, m, 1);
        UUID cancelled = confirmedBooking(c, b);
        post(client(), "/api/bookings/" + cancelled + "/cancel", Map.of(), c.token());
        moveTo(cancelled, m.atDay(5).atTime(10, 0));
        String t = admin().token();
        List<Map> rows = getList("/api/admin/reports/top-shops?month=" + m + "&limit=50", t);
        Set<String> mineIds = Set.of(a.shopId().toString(), b.shopId().toString());
        List<Map> mine = rows.stream().filter(r -> mineIds.contains(r.get("shopId"))).toList();
        assertThat(mine).extracting(r -> r.get("shopId")).containsExactly(a.shopId().toString(), b.shopId().toString());
        assertThat(mine).extracting(r -> r.get("bookings")).containsExactly(2, 1);
        assertThat(rows).extracting(r -> ((Number) r.get("bookings")).longValue())
                .isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(getList("/api/admin/reports/top-shops?month=" + m + "&limit=1", t)).hasSize(1);
        for (String bad : List.of("month=" + m + "&limit=0", "month=" + m + "&limit=51", "month=2026-13")) {
            assertThat(get(client(), "/api/admin/reports/top-shops?" + bad, t).getStatusCode().value())
                    .as(bad).isEqualTo(400);
        }
        assertThat(get(client(), "/api/admin/reports/finance?month=2026-13", t).getStatusCode().value()).isEqualTo(400);
    }

    private Map finance(String month, String token) {
        return get(client(), "/api/admin/reports/finance" + (month == null ? "" : "?month=" + month), token).getBody();
    }

    private static BigDecimal delta(Map after, Map before, String key) {
        return new BigDecimal(after.get(key).toString()).subtract(new BigDecimal(before.get(key).toString()));
    }

    private void setTopUpPaidAt(UUID topUpId, LocalDateTime vn) {
        jdbc.update("UPDATE sporthub.coin_top_ups SET paid_at = ? WHERE id = ?",
                vn.atZone(VN).toInstant().atOffset(ZoneOffset.UTC), topUpId);
    }

    private String withdraw(BookableCourt bc, long coins) {
        var r = post(client(), "/api/provider/withdrawals", Map.of("coinAmount", coins, "bankName", "VCB",
                "bankAccountNumber", "0123456789", "bankAccountName", "NGUYEN VAN A"), bc.provider().token());
        assertThat(r.getStatusCode().value()).isEqualTo(201);
        return (String) r.getBody().get("id");
    }

    // Kiểm tra: nạp/phí/thực chuyển tính theo paid_at trong tháng giờ VN (23:30 cuối tháng vào, 00:30 tháng sau ra).
    @Test
    void financeMatchesSeed() {
        YearMonth m = randomPastMonth();
        String t = admin().token();
        Map f0 = finance(m.toString(), t);
        Customer c = customer();
        setTopUpPaidAt(seedCoins(c.userId(), 300), m.atDay(10).atTime(12, 0));
        setTopUpPaidAt(seedCoins(c.userId(), 7), m.atEndOfMonth().atTime(23, 30));
        setTopUpPaidAt(seedCoins(c.userId(), 11), m.plusMonths(1).atDay(1).atTime(0, 30));
        BookableCourt bc = bookableCourt();
        seedShopCoins(bc, 1100);
        String paid = withdraw(bc, 1000);
        assertThat(post(client(), "/api/admin/withdrawals/" + paid + "/mark-paid", Map.of("bankTransactionRef", "FT1"), t)
                .getStatusCode().value()).isEqualTo(200);
        var inM = m.atDay(10).atTime(12, 0).atZone(VN).toInstant().atOffset(ZoneOffset.UTC);
        jdbc.update("UPDATE sporthub.shop_withdrawals SET paid_at = ?, reviewed_at = ? WHERE id = ?::uuid", inM, inM, paid);
        String rejected = withdraw(bc, 100);
        assertThat(post(client(), "/api/admin/withdrawals/" + rejected + "/reject", Map.of("reason", "x"), t)
                .getStatusCode().value()).isEqualTo(200);
        Map f = finance(m.toString(), t);
        assertThat(f.get("month")).isEqualTo(m.toString());
        assertThat(delta(f, f0, "customerTopUpVnd")).isEqualByComparingTo("307000");
        assertThat(delta(f, f0, "providerFeeVnd")).isEqualByComparingTo("70000");
        assertThat(delta(f, f0, "providerPayoutVnd")).isEqualByComparingTo("930000");
    }

    // Kiểm tra: số dư ví shop và coin đang chờ rút là số hiện tại, tăng đúng sau seed + rút PENDING.
    @Test
    void financeCurrentTotalsMove() {
        String t = admin().token();
        BookableCourt bc = bookableCourt();
        Map f0 = finance(null, t);
        seedShopCoins(bc, 200);
        withdraw(bc, 100);
        Map f1 = finance(null, t);
        assertThat(f1.get("month")).isEqualTo(YearMonth.now(VN).toString());
        assertThat(delta(f1, f0, "pendingWithdrawalCoins")).isEqualByComparingTo("100");
        assertThat(delta(f1, f0, "shopWalletCoins")).isEqualByComparingTo("100");
    }

    @Test
    void providerForbidden() {
        assertThat(get(client(), "/api/admin/reports/finance", bookableCourt().provider().token())
                .getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void customerForbidden() {
        assertThat(get(client(), "/api/provider/reports/monthly-bookings", customer().token()).getStatusCode().value())
                .isEqualTo(403);
    }
}
