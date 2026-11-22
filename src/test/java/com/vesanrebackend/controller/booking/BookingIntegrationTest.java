package com.vesanrebackend.controller.booking;

import com.vesanrebackend.BookingApiTestSupport;
import com.vesanrebackend.service.booking.BookingCompletionJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.time.Instant;
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
 * API: POST/GET /api/bookings, POST /api/bookings/{id}/cancel, /api/provider/bookings, /api/provider/wallet.
 */
class BookingIntegrationTest extends BookingApiTestSupport {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private BookingCompletionJob completionJob;

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

    // Kiểm tra (spec case 4): khách hủy -> trả coin, nhả slot, history/outbox CANCELLED; hủy lần 2 -> 409.
    @Test
    @SuppressWarnings("unchecked")
    void customerCancelReturnsCoinsAndFreesSlot() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 100);
        String id = (String) book(c, bc.courtId(), tomorrow(), "18:00", "19:00").getBody().get("id");
        assertThat(balance(c.userId())).isEqualTo(70);
        ResponseEntity<Map> r = post(client(), "/api/bookings/" + id + "/cancel", Map.of(), c.token());
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody().get("status")).isEqualTo("CANCELLED");
        assertThat(r.getBody().get("cancellationSource")).isEqualTo("CUSTOMER");
        assertThat(balance(c.userId())).isEqualTo(100).isEqualTo(ledgerSum(c.userId()));
        assertThat(count("SELECT count(*) FROM sporthub.coin_ledger_entries WHERE booking_id = ?::uuid "
                + "AND entry_type = 'BOOKING_DEPOSIT_RETURN' AND amount = 30", id)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sporthub.court_schedule_blocks s JOIN sporthub.bookings k "
                + "ON k.schedule_block_id = s.id WHERE k.id = ?::uuid AND s.state = 'RELEASED' "
                + "AND s.released_at IS NOT NULL", id)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sporthub.booking_status_history WHERE booking_id = ?::uuid "
                + "AND from_status = 'CONFIRMED' AND to_status = 'CANCELLED'", id)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sporthub.outbox_events WHERE event_key = ?", id + ":CANCELLED"))
                .isEqualTo(1);
        List<Map<String, Object>> slots = (List<Map<String, Object>>) get(client(),
                "/api/catalog/courts/" + bc.courtId() + "/availability?date=" + tomorrow(), null).getBody().get("slots");
        assertThat(slots).filteredOn(s -> s.get("start").toString().startsWith("18:00"))
                .allMatch(s -> Boolean.TRUE.equals(s.get("available")));
        ResponseEntity<Map> again = post(client(), "/api/bookings/" + id + "/cancel", Map.of(), c.token());
        assertThat(again.getStatusCode().value()).isEqualTo(409);
        assertThat(detail(again)).isEqualTo("Booking is CANCELLED");
    }

    // Kiểm tra (Review Focus 2): đúng lúc bắt đầu và sau đó không hủy được; số dư không đổi.
    @Test
    void cannotCancelAtOrAfterStart() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 100);
        Map<?, ?> b = book(c, bc.courtId(), tomorrow(), "18:00", "19:00").getBody();
        String id = (String) b.get("id");
        Instant start = Instant.parse((String) b.get("startAt"));
        testClock.set(start);
        assertThat(post(client(), "/api/bookings/" + id + "/cancel", Map.of(), c.token()).getStatusCode().value())
                .isEqualTo(409);
        testClock.set(start.plusSeconds(60));
        assertThat(post(client(), "/api/bookings/" + id + "/cancel", Map.of(), c.token()).getStatusCode().value())
                .isEqualTo(409);
        assertThat(balance(c.userId())).isEqualTo(70);
    }

    // Kiểm tra (Review Focus 3): venue SUSPENDED / court INACTIVE sau khi đặt vẫn hủy được, trả đủ coin.
    @Test
    void cancelAfterVenueSuspended() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 100);
        String id = (String) book(c, bc.courtId(), tomorrow(), "18:00", "19:00").getBody().get("id");
        jdbc.update("UPDATE sporthub.venues SET status = 'SUSPENDED' WHERE id = ?", bc.venueId());
        jdbc.update("UPDATE sporthub.courts SET status = 'INACTIVE' WHERE id = ?", bc.courtId());
        assertThat(post(client(), "/api/bookings/" + id + "/cancel", Map.of(), c.token()).getStatusCode().value())
                .isEqualTo(200);
        assertThat(balance(c.userId())).isEqualTo(100);
    }

    // Kiểm tra (spec case 5): provider chủ venue hủy được, provider khác 404, reason rỗng 400.
    @Test
    void providerCancelsOwnVenueOnly() {
        BookableCourt bc = bookableCourt();
        Provider other = verifiedProvider();
        Customer c = customer();
        seedCoins(c.userId(), 100);
        String id = (String) book(c, bc.courtId(), tomorrow(), "18:00", "19:00").getBody().get("id");
        RestClient client = client();
        assertThat(get(client, "/api/provider/bookings/" + id, other.token()).getStatusCode().value()).isEqualTo(404);
        assertThat(post(client, "/api/provider/bookings/" + id + "/cancel", Map.of("reason", "x"), other.token())
                .getStatusCode().value()).isEqualTo(404);
        assertThat(post(client, "/api/provider/bookings/" + id + "/cancel", Map.of("reason", ""),
                bc.provider().token()).getStatusCode().value()).isEqualTo(400);
        ResponseEntity<Map> r = post(client, "/api/provider/bookings/" + id + "/cancel",
                Map.of("reason", "Court flooded"), bc.provider().token());
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody().get("cancellationSource")).isEqualTo("PROVIDER");
        assertThat(r.getBody().get("cancellationReason")).isEqualTo("Court flooded");
        assertThat(balance(c.userId())).isEqualTo(100);
    }

    // Kiểm tra: provider liệt kê theo venue+date (đơn đặt sau lên đầu dù giờ chơi muộn hơn), thấy customer;
    // date thiếu venue -> 400; ví shop 0.
    @Test
    @SuppressWarnings("unchecked")
    void providerListsAndSeesCustomer() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 100);
        String first = (String) book(c, bc.courtId(), tomorrow(), "18:00", "19:00").getBody().get("id");
        String id = (String) book(c, bc.courtId(), tomorrow(), "20:00", "21:00").getBody().get("id");
        RestClient client = client();
        ResponseEntity<Map> list = get(client, "/api/provider/bookings?venueId=" + bc.venueId() + "&date="
                + tomorrow(), bc.provider().token());
        assertThat(list.getStatusCode().value()).isEqualTo(200);
        List<Map<String, Object>> items = (List<Map<String, Object>>) list.getBody().get("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("id")).isEqualTo(id);
        assertThat(items.get(1).get("id")).isEqualTo(first);
        assertThat(get(client, "/api/provider/bookings?date=" + tomorrow(), bc.provider().token())
                .getStatusCode().value()).isEqualTo(400);
        Map<String, Object> detail = get(client, "/api/provider/bookings/" + id, bc.provider().token()).getBody();
        assertThat(((Map<String, Object>) detail.get("customer")).get("displayName")).isEqualTo("IT Customer");
        ResponseEntity<Map> wallet = get(client, "/api/provider/wallet", bc.provider().token());
        assertThat(wallet.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) wallet.getBody().get("balance")).longValue()).isZero();
    }

    // Kiểm tra (spec case 7): hết giờ -> job trả cọc vào ví shop đúng một lần; booking đã hủy không bị đụng.
    @Test
    void completionPaysShopOnce() {
        BookableCourt bc = bookableCourt();
        BookableCourt other = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 200);
        String id = (String) book(c, bc.courtId(), tomorrow(), "18:00", "19:00").getBody().get("id");
        String cancelled = (String) book(c, other.courtId(), tomorrow(), "18:00", "19:00").getBody().get("id");
        post(client(), "/api/bookings/" + cancelled + "/cancel", Map.of(), c.token());
        Instant endAt = jdbc.queryForObject("SELECT s.end_at FROM sporthub.court_schedule_blocks s JOIN "
                + "sporthub.bookings k ON k.schedule_block_id = s.id WHERE k.id = ?::uuid", Instant.class, id);
        testClock.set(endAt.plusSeconds(60));
        // Shared DB may hold >100 older CONFIRMED bookings ahead of ours.
        for (int i = 0; i < 50 && !"COMPLETED".equals(jdbc.queryForObject(
                "SELECT status FROM sporthub.bookings WHERE id = ?::uuid", String.class, id)); i++) {
            completionJob.run();
        }
        assertThat(jdbc.queryForObject("SELECT status FROM sporthub.bookings WHERE id = ?::uuid", String.class, id))
                .isEqualTo("COMPLETED");
        assertThat(count("SELECT count(*) FROM sporthub.bookings WHERE id = ?::uuid AND completed_at IS NOT NULL", id))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sporthub.court_schedule_blocks s JOIN sporthub.bookings k "
                + "ON k.schedule_block_id = s.id WHERE k.id = ?::uuid AND s.state = 'ACTIVE'", id)).isEqualTo(1);
        assertThat(shopBalance(bc.shopId())).isEqualTo(30);
        assertThat(count("SELECT count(*) FROM sporthub.coin_ledger_entries WHERE booking_id = ?::uuid "
                + "AND entry_type = 'BOOKING_PAYOUT' AND amount = 30", id)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM sporthub.outbox_events WHERE event_key = ?", id + ":COMPLETED"))
                .isEqualTo(1);
        completionJob.run();
        assertThat(shopBalance(bc.shopId())).isEqualTo(30);
        assertThat(jdbc.queryForObject("SELECT status FROM sporthub.bookings WHERE id = ?::uuid", String.class,
                cancelled)).isEqualTo("CANCELLED");
        assertThat(shopBalance(other.shopId())).isZero();
    }
}
