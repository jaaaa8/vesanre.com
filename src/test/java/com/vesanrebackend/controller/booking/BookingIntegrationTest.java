package com.vesanrebackend.controller.booking;

import com.vesanrebackend.BookingApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loại test: integration HTTP thật + PostgreSQL (kế thừa BookingApiTestSupport).
 * API: POST/GET /api/bookings.
 */
class BookingIntegrationTest extends BookingApiTestSupport {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private LocalDate tomorrow() {
        return LocalDate.now(VN).plusDays(1);
    }

    private ResponseEntity<Map> book(Customer c, UUID courtId, LocalDate date, String start, String end) {
        Map<String, Object> body = new HashMap<>();
        body.put("courtId", courtId.toString());
        body.put("date", date.toString());
        body.put("startTime", start);
        body.put("endTime", end);
        return post(client(), "/api/bookings", body, c.token());
    }

    private String detail(ResponseEntity<Map> r) {
        return (String) r.getBody().get("detail");
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    // Kiểm tra (spec case 1): đặt 18:00-19:00 trừ 30 coin, ghi ledger/block/history/outbox, slot bị chiếm.
    @Test
    @SuppressWarnings("unchecked")
    void createConfirmsAndDeductsCoins() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 100);
        ResponseEntity<Map> r = book(c, bc.courtId(), tomorrow(), "18:00", "19:00");
        assertThat(r.getStatusCode().value()).isEqualTo(201);
        Map<String, Object> b = r.getBody();
        assertThat(b.get("status")).isEqualTo("CONFIRMED");
        assertThat(((Number) b.get("depositCoins")).longValue()).isEqualTo(30);
        assertThat(((Number) b.get("depositPercent")).intValue()).isEqualTo(30);
        assertThat((List<?>) b.get("priceBreakdown")).hasSize(2);
        assertThat(b.get("customer")).isNull();
        String id = (String) b.get("id");
        assertThat(balance(c.userId())).isEqualTo(70).isEqualTo(ledgerSum(c.userId()) );
        assertThat(count("SELECT count(*) FROM sporthub.coin_ledger_entries WHERE booking_id = ?::uuid "
                + "AND entry_type = 'BOOKING_DEPOSIT' AND amount = -30", id)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sporthub.court_schedule_blocks s JOIN sporthub.bookings k "
                + "ON k.schedule_block_id = s.id WHERE k.id = ?::uuid AND s.state = 'ACTIVE' "
                + "AND s.block_kind = 'BOOKING'", id)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sporthub.booking_status_history WHERE booking_id = ?::uuid "
                + "AND from_status IS NULL AND to_status = 'CONFIRMED'", id)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sporthub.outbox_events WHERE event_key = ?", id + ":CONFIRMED"))
                .isEqualTo(1);

        Map<String, Object> av = get(client(), "/api/catalog/courts/" + bc.courtId() + "/availability?date="
                + tomorrow(), null).getBody();
        List<Map<String, Object>> slots = (List<Map<String, Object>>) av.get("slots");
        assertThat(slots).filteredOn(s -> s.get("start").toString().startsWith("18:00")
                || s.get("start").toString().startsWith("18:30")).hasSize(2)
                .allMatch(s -> Boolean.FALSE.equals(s.get("available")));
    }

    // Kiểm tra (Review Focus 4): số dư đúng bằng cọc vẫn đặt được, còn 0.
    @Test
    void exactBalanceIsEnough() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 30);
        assertThat(book(c, bc.courtId(), tomorrow(), "18:00", "19:00").getStatusCode().value()).isEqualTo(201);
        assertThat(balance(c.userId())).isZero();
    }

    // Kiểm tra (spec case 3): thiếu coin -> 409, không tạo block, số dư giữ nguyên.
    @Test
    void insufficientCoinsCreatesNothing() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 29);
        ResponseEntity<Map> r = book(c, bc.courtId(), tomorrow(), "18:00", "19:00");
        assertThat(r.getStatusCode().value()).isEqualTo(409);
        assertThat(detail(r)).isEqualTo("Insufficient coins");
        assertThat(count("SELECT count(*) FROM sporthub.court_schedule_blocks WHERE court_id = ?", bc.courtId()))
                .isZero();
        assertThat(balance(c.userId())).isEqualTo(29);
    }

    // Kiểm tra (spec case 2): hai khách đặt đồng thời cùng / chồng khoảng -> đúng một 201 và một 409.
    @Test
    void concurrentSameAndOverlappingSlots() throws Exception {
        BookableCourt same = bookableCourt();
        BookableCourt overlap = bookableCourt();
        Customer a = customer();
        Customer b = customer();
        seedCoins(a.userId(), 100);
        seedCoins(b.userId(), 100);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (Object[] c : new Object[][]{{same, "18:00", "19:00", "18:00", "19:00"},
                    {overlap, "18:00", "19:00", "18:30", "19:30"}}) {
                BookableCourt court = (BookableCourt) c[0];
                CountDownLatch go = new CountDownLatch(1);
                Future<ResponseEntity<Map>> fa = pool.submit(() -> {
                    go.await();
                    return book(a, court.courtId(), tomorrow(), (String) c[1], (String) c[2]);
                });
                Future<ResponseEntity<Map>> fb = pool.submit(() -> {
                    go.await();
                    return book(b, court.courtId(), tomorrow(), (String) c[3], (String) c[4]);
                });
                go.countDown();
                ResponseEntity<Map> ra = fa.get();
                ResponseEntity<Map> rb = fb.get();
                List<Integer> codes = List.of(ra.getStatusCode().value(), rb.getStatusCode().value());
                assertThat(codes).containsExactlyInAnyOrder(201, 409);
                ResponseEntity<Map> loser = ra.getStatusCode().value() == 409 ? ra : rb;
                assertThat(detail(loser)).isEqualTo("Slot is no longer available");
                assertThat(count("SELECT count(*) FROM sporthub.court_schedule_blocks WHERE court_id = ? "
                        + "AND state = 'ACTIVE'", court.courtId())).isEqualTo(1);
                Customer loserCustomer = loser == ra ? a : b;
                // The loser's wallet only dropped by earlier winning bookings on the other court, never by this one.
                assertThat(count("SELECT count(*) FROM sporthub.bookings k JOIN sporthub.court_schedule_blocks s "
                        + "ON s.id = k.schedule_block_id WHERE s.court_id = ? AND k.customer_id = ?",
                        court.courtId(), loserCustomer.userId())).isZero();
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(balance(a.userId())).isEqualTo(ledgerSum(a.userId()));
        assertThat(balance(b.userId())).isEqualTo(ledgerSum(b.userId()));
        assertThat(balance(a.userId()) + balance(b.userId())).isEqualTo(200 - 60);
    }

    // Kiểm tra: ledger entries hiện đúng bookingCode (lazy booking không vỡ).
    @Test
    @SuppressWarnings("unchecked")
    void entriesShowBookingCode() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 100);
        String code = (String) book(c, bc.courtId(), tomorrow(), "18:00", "19:00").getBody().get("bookingCode");
        List<Map<String, Object>> items = (List<Map<String, Object>>) get(client(), "/api/wallet/entries", c.token())
                .getBody().get("items");
        assertThat(items.get(0).get("type")).isEqualTo("BOOKING_DEPOSIT");
        assertThat(((Number) items.get(0).get("amount")).longValue()).isEqualTo(-30);
        assertThat(items.get(0).get("bookingCode")).isEqualTo(code);
    }

    // Kiểm tra (spec case 6): khách khác không thấy booking của người khác (404, không có trong list).
    @Test
    @SuppressWarnings("unchecked")
    void cannotSeeOthersBooking() {
        BookableCourt bc = bookableCourt();
        Customer a = customer();
        Customer b = customer();
        seedCoins(a.userId(), 100);
        String id = (String) book(a, bc.courtId(), tomorrow(), "18:00", "19:00").getBody().get("id");
        assertThat(get(client(), "/api/bookings/" + id, a.token()).getStatusCode().value()).isEqualTo(200);
        assertThat(get(client(), "/api/bookings/" + id, b.token()).getStatusCode().value()).isEqualTo(404);
        List<Map<String, Object>> items = (List<Map<String, Object>>) get(client(), "/api/bookings", b.token())
                .getBody().get("items");
        assertThat(items).extracting(i -> i.get("id")).doesNotContain(id);
        List<Map<String, Object>> mine = (List<Map<String, Object>>) get(client(), "/api/bookings?status=CONFIRMED",
                a.token()).getBody().get("items");
        assertThat(mine).extracting(i -> i.get("id")).contains(id);
    }

    // Kiểm tra: court không tồn tại 404; ngày +31 400; 30 phút 400; lệch bước 400; venue SUSPENDED 404.
    @Test
    void rejectsBadRequests() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 100);
        assertThat(book(c, UUID.randomUUID(), tomorrow(), "18:00", "19:00").getStatusCode().value()).isEqualTo(404);
        assertThat(book(c, bc.courtId(), LocalDate.now(VN).plusDays(31), "18:00", "19:00").getStatusCode().value())
                .isEqualTo(400);
        assertThat(book(c, bc.courtId(), tomorrow(), "18:00", "18:30").getStatusCode().value()).isEqualTo(400);
        ResponseEntity<Map> off = book(c, bc.courtId(), tomorrow(), "17:45", "18:45");
        assertThat(off.getStatusCode().value()).isEqualTo(400);
        assertThat(detail(off)).isEqualTo("Requested time is not bookable");
        ResponseEntity<Map> backwards = book(c, bc.courtId(), tomorrow(), "19:00", "18:00");
        assertThat(backwards.getStatusCode().value()).isEqualTo(400);
        jdbc.update("UPDATE sporthub.venues SET status = 'SUSPENDED' WHERE id = ?", bc.venueId());
        assertThat(book(c, bc.courtId(), tomorrow(), "18:00", "19:00").getStatusCode().value()).isEqualTo(404);
        assertThat(balance(c.userId())).isEqualTo(100);
    }
}
