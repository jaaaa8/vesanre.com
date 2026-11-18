package com.vesanrebackend.controller.wallet;

import com.vesanrebackend.BookingApiTestSupport;
import com.vesanrebackend.service.coin.TopUpExpiryJob;
import com.vesanrebackend.service.payment.FakeTopUpGateway;
import com.vesanrebackend.service.payment.GatewayPayment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loại test: integration HTTP thật + PostgreSQL (kế thừa BookingApiTestSupport), cổng FAKE bật.
 * API: POST/GET /api/wallet/top-ups, POST /api/payment/webhooks/{provider}, trang /api/payment/fake/**.
 */
@TestPropertySource(properties = {"app.payment.fake.enabled=true", "app.payment.fake.secret=test-secret",
        "app.payment.fake.base-url=http://test"})
class TopUpIntegrationTest extends BookingApiTestSupport {

    @Autowired
    private FakeTopUpGateway gateway;

    @Autowired
    private TopUpExpiryJob expiryJob;

    private ResponseEntity<Map> createTopUp(Customer c, long coins) {
        return post(client(), "/api/wallet/top-ups", Map.of("coins", coins), c.token());
    }

    // Raw-body webhook, no token, like a real gateway.
    private ResponseEntity<String> webhook(String provider, byte[] body, String signature) {
        return client().post().uri("/api/payment/webhooks/" + provider).contentType(MediaType.APPLICATION_JSON)
                .headers(h -> {
                    if (signature != null) {
                        h.add("X-Signature", signature);
                    }
                })
                .body(body).retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> pay(String code, long amountVnd, String eventId) {
        byte[] body = gateway.payload(new GatewayPayment(eventId, "tx_" + UUID.randomUUID(), code, amountVnd,
                testClock.instant()));
        return webhook("FAKE", body, gateway.sign(body));
    }

    private ResponseEntity<String> pay(Map<String, Object> top, String eventId) {
        return pay((String) top.get("transferCode"), ((Number) top.get("coins")).longValue() * 1000, eventId);
    }

    // The default client follows redirects; the fake page flow needs to see the 303.
    private RestClient noRedirectClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port)
                .requestFactory(new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()))
                .defaultStatusHandler(org.springframework.http.HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    private ResponseEntity<String> page(String code) {
        return noRedirectClient().get().uri("/api/payment/fake/" + code).retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> confirm(String code) {
        return noRedirectClient().post().uri("/api/payment/fake/" + code + "/confirm").retrieve().toEntity(String.class);
    }

    private String evt() {
        return "evt_" + UUID.randomUUID();
    }

    private Map<String, Object> event(String eventId) {
        return jdbc.queryForMap("SELECT status, error_message, processed_at, event_type FROM sporthub.payment_webhook_events "
                + "WHERE provider_code = 'FAKE' AND provider_event_id = ?", eventId);
    }

    private long eventCount(String eventId) {
        return jdbc.queryForObject("SELECT count(*) FROM sporthub.payment_webhook_events WHERE provider_code = 'FAKE' "
                + "AND provider_event_id = ?", Long.class, eventId);
    }

    // Kiểm tra: tạo lệnh 100 coin -> 201, PENDING, 100000đ, mã VSR..., paymentUrl, hết hạn sau 15 phút theo Clock.
    @Test
    void createTopUp() {
        Instant now = Instant.parse("2026-11-21T10:00:00Z");
        testClock.set(now);
        Customer c = customer();
        ResponseEntity<Map> r = createTopUp(c, 100);
        assertThat(r.getStatusCode().value()).isEqualTo(201);
        Map<String, Object> b = r.getBody();
        assertThat(b.get("status")).isEqualTo("PENDING");
        assertThat(((Number) b.get("amountVnd")).longValue()).isEqualTo(100000);
        assertThat(((Number) b.get("coins")).longValue()).isEqualTo(100);
        String code = (String) b.get("transferCode");
        assertThat(code).matches("^VSR[A-Z0-9]{8}$");
        assertThat(b.get("paymentUrl")).isEqualTo("http://test/api/payment/fake/" + code);
        assertThat(Instant.parse((String) b.get("expiresAt"))).isEqualTo(now.plusSeconds(15 * 60));
        assertThat(b.get("paidAt")).isNull();

        ResponseEntity<Map> g = get(client(), "/api/wallet/top-ups/" + b.get("id"), c.token());
        assertThat(g.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) g.getBody().get("amountVnd")).longValue()).isEqualTo(100000);
        assertThat(g.getBody().get("paymentUrl")).isEqualTo(b.get("paymentUrl"));
    }

    // Kiểm tra (Review Focus 4): coins ngoài khoảng 10..5000, thiếu hoặc null -> 400.
    @Test
    void coinsOutOfRange() {
        Customer c = customer();
        assertThat(createTopUp(c, 9).getStatusCode().value()).isEqualTo(400);
        assertThat(createTopUp(c, 5001).getStatusCode().value()).isEqualTo(400);
        assertThat(post(client(), "/api/wallet/top-ups", Map.of(), c.token()).getStatusCode().value()).isEqualTo(400);
        Map<String, Object> nullCoins = new HashMap<>();
        nullCoins.put("coins", null);
        assertThat(post(client(), "/api/wallet/top-ups", nullCoins, c.token()).getStatusCode().value()).isEqualTo(400);
    }

    // Kiểm tra: khách B xem lệnh của khách A -> 404.
    @Test
    void otherUsersTopUpIs404() {
        Customer a = customer();
        Customer b = customer();
        String id = (String) createTopUp(a, 50).getBody().get("id");
        assertThat(get(client(), "/api/wallet/top-ups/" + id, b.token()).getStatusCode().value()).isEqualTo(404);
        assertThat(get(client(), "/api/wallet/top-ups/" + id, a.token()).getStatusCode().value()).isEqualTo(200);
    }

    // Kiểm tra: chưa đăng nhập -> 401.
    @Test
    void topUpRequiresLogin() {
        RestClient client = client();
        assertThat(post(client, "/api/wallet/top-ups", Map.of("coins", 100)).getStatusCode().value()).isEqualTo(401);
    }

    // Kiểm tra: webhook đúng -> 200, lệnh PAID (paidAt theo Clock), ví +100, 1 ledger TOP_UP, event PROCESSED.
    @Test
    void validWebhookCredits() {
        Instant now = Instant.parse("2026-11-21T10:00:00Z");
        testClock.set(now);
        Customer c = customer();
        Map<String, Object> top = createTopUp(c, 100).getBody();
        String e = evt();
        assertThat(pay(top, e).getStatusCode().value()).isEqualTo(200);
        Map<?, ?> g = get(client(), "/api/wallet/top-ups/" + top.get("id"), c.token()).getBody();
        assertThat(g.get("status")).isEqualTo("PAID");
        assertThat(Instant.parse((String) g.get("paidAt"))).isEqualTo(now);
        assertThat(balance(c.userId())).isEqualTo(100).isEqualTo(ledgerSum(c.userId()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sporthub.coin_ledger_entries WHERE top_up_id = ?::uuid "
                + "AND entry_type = 'TOP_UP'", Long.class, top.get("id"))).isEqualTo(1);
        Map<String, Object> ev = event(e);
        assertThat(ev.get("status")).isEqualTo("PROCESSED");
        assertThat(ev.get("processed_at")).isNotNull();
        assertThat(ev.get("event_type")).isEqualTo("TOP_UP_PAID");
    }

    // Kiểm tra: cùng event gửi 2 lần tuần tự, và 2 luồng đồng thời -> cộng đúng một lần, 1 event.
    @Test
    void duplicateEventCreditsOnce() throws Exception {
        Customer c = customer();
        Map<String, Object> top = createTopUp(c, 100).getBody();
        String e = evt();
        assertThat(pay(top, e).getStatusCode().value()).isEqualTo(200);
        assertThat(pay(top, e).getStatusCode().value()).isEqualTo(200);
        assertThat(balance(c.userId())).isEqualTo(100);
        assertThat(eventCount(e)).isEqualTo(1);

        Map<String, Object> top2 = createTopUp(c, 50).getBody();
        String e2 = evt();
        byte[] body = gateway.payload(new GatewayPayment(e2, "tx_" + UUID.randomUUID(),
                (String) top2.get("transferCode"), 50000, testClock.instant()));
        String sig = gateway.sign(body);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch go = new CountDownLatch(1);
            Future<ResponseEntity<String>> f1 = pool.submit(() -> {
                go.await();
                return webhook("FAKE", body, sig);
            });
            Future<ResponseEntity<String>> f2 = pool.submit(() -> {
                go.await();
                return webhook("FAKE", body, sig);
            });
            go.countDown();
            assertThat(f1.get(15, TimeUnit.SECONDS).getStatusCode().value()).isEqualTo(200);
            assertThat(f2.get(15, TimeUnit.SECONDS).getStatusCode().value()).isEqualTo(200);
        } finally {
            pool.shutdownNow();
        }
        assertThat(balance(c.userId())).isEqualTo(150).isEqualTo(ledgerSum(c.userId()));
        assertThat(eventCount(e2)).isEqualTo(1);
    }

    // Kiểm tra: event khác cùng transferCode -> 200, IGNORED có error_message, số dư không đổi.
    @Test
    void secondEventSameTopUpIgnored() {
        Customer c = customer();
        Map<String, Object> top = createTopUp(c, 100).getBody();
        assertThat(pay(top, evt()).getStatusCode().value()).isEqualTo(200);
        String e2 = evt();
        assertThat(pay(top, e2).getStatusCode().value()).isEqualTo(200);
        assertThat(event(e2).get("status")).isEqualTo("IGNORED");
        assertThat(event(e2).get("error_message")).isNotNull();
        assertThat(balance(c.userId())).isEqualTo(100);
    }

    // Kiểm tra: chữ ký sai (hoặc không phải hex, hoặc thiếu) -> 401, không lưu event.
    @Test
    void badSignature401NothingStored() {
        Customer c = customer();
        Map<String, Object> top = createTopUp(c, 100).getBody();
        String e = evt();
        byte[] body = gateway.payload(new GatewayPayment(e, "tx_1", (String) top.get("transferCode"), 100000,
                testClock.instant()));
        ResponseEntity<String> r = webhook("FAKE", body, "0".repeat(64));
        assertThat(r.getStatusCode().value()).isEqualTo(401);
        assertThat(r.getBody()).contains("Invalid signature");
        assertThat(webhook("FAKE", body, "zz").getStatusCode().value()).isEqualTo(401);
        assertThat(webhook("FAKE", body, null).getStatusCode().value()).isEqualTo(401);
        assertThat(eventCount(e)).isZero();
        assertThat(balance(c.userId())).isZero();
    }

    // Kiểm tra: sai số tiền, mã không tồn tại -> 200, IGNORED, ví không đổi.
    @Test
    void wrongAmountOrUnknownCodeIgnored() {
        Customer c = customer();
        Map<String, Object> top = createTopUp(c, 100).getBody();
        String e1 = evt();
        String e2 = evt();
        assertThat(pay((String) top.get("transferCode"), 99000, e1).getStatusCode().value()).isEqualTo(200);
        assertThat(pay("VSRNOSUCH01", 100000, e2).getStatusCode().value()).isEqualTo(200);
        assertThat(event(e1).get("status")).isEqualTo("IGNORED");
        assertThat(event(e2).get("status")).isEqualTo("IGNORED");
        assertThat(balance(c.userId())).isZero();
        assertThat(get(client(), "/api/wallet/top-ups/" + top.get("id"), c.token()).getBody().get("status"))
                .isEqualTo("PENDING");
    }

    // Kiểm tra (Review Focus 2): transferCode viết thường không khớp -> IGNORED.
    @Test
    void lowercaseCodeIgnored() {
        Customer c = customer();
        Map<String, Object> top = createTopUp(c, 100).getBody();
        String e = evt();
        assertThat(pay(((String) top.get("transferCode")).toLowerCase(), 100000, e).getStatusCode().value()).isEqualTo(200);
        assertThat(event(e).get("status")).isEqualTo("IGNORED");
        assertThat(balance(c.userId())).isZero();
    }

    // Kiểm tra (Review Focus 1): payload ký đúng nhưng thiếu field -> 400, không lưu event.
    @Test
    void malformedPayload400() {
        byte[] body = "{\"eventId\":\"x-malformed\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(webhook("FAKE", body, gateway.sign(body)).getStatusCode().value()).isEqualTo(400);
        assertThat(eventCount("x-malformed")).isZero();
    }

    // Kiểm tra (spec 10.11): webhook chờ khóa ví đang bị giữ, không ghi đè số dư cũ -> 170 = tổng ledger.
    @Test
    void webhookWaitsForHeldWalletLock() throws Exception {
        Customer c = customer();
        seedCoins(c.userId(), 100);
        Map<String, Object> top = createTopUp(c, 50).getBody();
        CompletableFuture<ResponseEntity<String>> hook = new CompletableFuture<>();
        tx.executeWithoutResult(s -> {
            seedCoins(c.userId(), 20); // joins this transaction and keeps the wallet row locked
            CompletableFuture.supplyAsync(() -> pay(top, evt())).whenComplete((r, ex) -> {
                if (ex != null) {
                    hook.completeExceptionally(ex);
                } else {
                    hook.complete(r);
                }
            });
            try {
                awaitLockWaiter();
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
            assertThat(hook).isNotDone();
        });
        assertThat(hook.get(15, TimeUnit.SECONDS).getStatusCode().value()).isEqualTo(200);
        assertThat(balance(c.userId())).isEqualTo(170).isEqualTo(ledgerSum(c.userId()));
    }

    // Kiểm tra: lệnh của provider khác (TEST) nhận webhook FAKE -> IGNORED.
    @Test
    void providerMismatchIgnored() {
        Customer c = customer();
        UUID walletId = tx.execute(s -> coinWallets.lockUserWallet(c.userId()).getId());
        String code = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        jdbc.update("INSERT INTO sporthub.coin_top_ups (id, wallet_id, transfer_code, coin_amount, amount_vnd, status, "
                + "provider_code, expires_at) VALUES (?, ?, ?, 100, 100000, 'PENDING', 'TEST', now() + interval '1 day')",
                UUID.randomUUID(), walletId, code);
        String e = evt();
        assertThat(pay(code, 100000, e).getStatusCode().value()).isEqualTo(200);
        assertThat(event(e).get("status")).isEqualTo("IGNORED");
        assertThat(balance(c.userId())).isZero();
    }

    // Kiểm tra: provider không có cổng -> 404.
    @Test
    void unknownProvider404() {
        assertThat(webhook("SEPAY", "{}".getBytes(StandardCharsets.UTF_8), "00").getStatusCode().value()).isEqualTo(404);
    }

    // Kiểm tra: đặt sân (cọc 30) và webhook nạp 50 chạy đồng thời -> 100 - 30 + 50 = 120 = tổng ledger.
    @Test
    void concurrentBookingAndTopUpNoLostUpdate() throws Exception {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 100);
        Map<String, Object> top = createTopUp(c, 50).getBody();
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        Map<String, Object> bookBody = Map.of("courtId", bc.courtId().toString(), "date", date.toString(),
                "startTime", "18:00", "endTime", "19:00");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch go = new CountDownLatch(1);
            Future<ResponseEntity<Map>> booking = pool.submit(() -> {
                go.await();
                return post(client(), "/api/bookings", bookBody, c.token());
            });
            Future<ResponseEntity<String>> hook = pool.submit(() -> {
                go.await();
                return pay(top, evt());
            });
            go.countDown();
            assertThat(booking.get(20, TimeUnit.SECONDS).getStatusCode().value()).isEqualTo(201);
            assertThat(hook.get(20, TimeUnit.SECONDS).getStatusCode().value()).isEqualTo(200);
        } finally {
            pool.shutdownNow();
        }
        assertThat(balance(c.userId())).isEqualTo(120).isEqualTo(ledgerSum(c.userId()));
    }

    // Kiểm tra: lệnh hết hạn đúng 15 phút (không sớm hơn); tiền về muộn vẫn PAID và cộng coin (T5).
    @Test
    void expiredTopUpStillCreditsOnLatePayment() {
        Instant t = Instant.parse("2026-11-21T10:00:00Z");
        testClock.set(t);
        Customer c = customer();
        Map<String, Object> top = createTopUp(c, 100).getBody();
        String path = "/api/wallet/top-ups/" + top.get("id");
        testClock.set(t.plusSeconds(14 * 60));
        expiryJob.run();
        assertThat(get(client(), path, c.token()).getBody().get("status")).isEqualTo("PENDING");
        testClock.set(t.plusSeconds(15 * 60));
        expiryJob.run();
        Map<?, ?> expired = get(client(), path, c.token()).getBody();
        assertThat(expired.get("status")).isEqualTo("EXPIRED");
        assertThat(expired.get("paidAt")).isNull();
        assertThat(pay(top, evt()).getStatusCode().value()).isEqualTo(200);
        assertThat(get(client(), path, c.token()).getBody().get("status")).isEqualTo("PAID");
        assertThat(balance(c.userId())).isEqualTo(100);
    }

    // Kiểm tra: trang giả lập PENDING có form; confirm -> 303; trang sau đó "Đã thanh toán" không form; ví +100.
    @Test
    void fakePageShowsPendingThenPaid() {
        Customer c = customer();
        String code = (String) createTopUp(c, 100).getBody().get("transferCode");
        ResponseEntity<String> before = page(code);
        assertThat(before.getStatusCode().value()).isEqualTo(200);
        assertThat(before.getHeaders().getContentType().toString()).startsWith("text/html");
        assertThat(before.getBody()).contains(code, "100.000", "<form");
        ResponseEntity<String> done = confirm(code);
        assertThat(done.getStatusCode().value()).isEqualTo(303);
        assertThat(done.getHeaders().getLocation().getPath()).isEqualTo("/api/payment/fake/" + code);
        ResponseEntity<String> after = page(code);
        assertThat(after.getBody()).contains("Đã thanh toán").doesNotContain("<form");
        assertThat(balance(c.userId())).isEqualTo(100);
    }

    // Kiểm tra: bấm confirm hai lần -> chỉ cộng một lần.
    @Test
    void confirmTwiceCreditsOnce() {
        Customer c = customer();
        String code = (String) createTopUp(c, 100).getBody().get("transferCode");
        assertThat(confirm(code).getStatusCode().value()).isEqualTo(303);
        assertThat(confirm(code).getStatusCode().value()).isEqualTo(303);
        assertThat(balance(c.userId())).isEqualTo(100).isEqualTo(ledgerSum(c.userId()));
    }

    // Kiểm tra (Review Focus 5): mã không tồn tại -> 404 HTML, không lộ stack trace; confirm cũng 404.
    @Test
    void unknownCodePage404() {
        ResponseEntity<String> r = page("VSRNOTEXIST");
        assertThat(r.getStatusCode().value()).isEqualTo(404);
        assertThat(r.getBody()).contains("Không tìm thấy").doesNotContain("Exception");
        assertThat(confirm("VSRNOTEXIST").getStatusCode().value()).isEqualTo(404);
    }

    // Kiểm tra: lệnh EXPIRED vẫn hiện nút (để demo tiền về muộn), confirm xong ví được cộng.
    @Test
    void expiredTopUpStillShowsButton() {
        Instant t = Instant.parse("2026-11-21T10:00:00Z");
        testClock.set(t);
        Customer c = customer();
        String code = (String) createTopUp(c, 100).getBody().get("transferCode");
        testClock.set(t.plusSeconds(16 * 60));
        expiryJob.run();
        String html = page(code).getBody();
        assertThat(html).contains("<form");
        assertThat(confirm(code).getStatusCode().value()).isEqualTo(303);
        assertThat(balance(c.userId())).isEqualTo(100);
    }
}
