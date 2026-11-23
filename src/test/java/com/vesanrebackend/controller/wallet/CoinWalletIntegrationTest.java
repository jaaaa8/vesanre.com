package com.vesanrebackend.controller.wallet;

import com.vesanrebackend.BookingApiTestSupport;
import com.vesanrebackend.entity.enums.CoinEntryType;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Loại test: integration HTTP thật + PostgreSQL (kế thừa BookingApiTestSupport).
 * API: GET /api/wallet, GET /api/wallet/entries; dịch vụ CoinWalletService.
 */
class CoinWalletIntegrationTest extends BookingApiTestSupport {
    // Kiểm tra (Review Focus 5): user chưa có ví -> balance 0, entries rỗng, và GET không tạo dòng ví.
    @Test
    void walletWithoutRowShowsZero() {
        RestClient client = client();
        Customer c = customer();
        ResponseEntity<Map> wallet = get(client, "/api/wallet", c.token());
        assertThat(wallet.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) wallet.getBody().get("balance")).longValue()).isZero();
        ResponseEntity<Map> entries = get(client, "/api/wallet/entries", c.token());
        assertThat(entries.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) entries.getBody().get("items")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sporthub.coin_wallets WHERE user_id = ?", Integer.class,
                c.userId())).isZero();
    }

    // Kiểm tra: nạp hai lần -> số dư = tổng sổ; entries mới nhất trước, balanceAfter đúng, bookingCode null.
    @Test
    @SuppressWarnings("unchecked")
    void topUpCreditsAndListsEntry() {
        Customer c = customer();
        seedCoins(c.userId(), 100);
        seedCoins(c.userId(), 100);
        assertThat(balance(c.userId())).isEqualTo(200).isEqualTo(ledgerSum(c.userId()));

        RestClient client = client();
        assertThat(((Number) get(client, "/api/wallet", c.token()).getBody().get("balance")).longValue()).isEqualTo(200);
        List<Map<String, Object>> items = (List<Map<String, Object>>) get(client, "/api/wallet/entries", c.token())
                .getBody().get("items");
        assertThat(items).hasSize(2);
        assertThat(items).extracting(i -> i.get("type")).containsOnly("TOP_UP");
        assertThat(items).extracting(i -> ((Number) i.get("amount")).longValue()).containsOnly(100L);
        assertThat(items).extracting(i -> ((Number) i.get("balanceAfter")).longValue()).containsExactly(200L, 100L);
        assertThat(items).extracting(i -> i.get("bookingCode")).containsOnlyNulls();
    }

    // Kiểm tra: một top-up không thể cộng coin hai lần (unique top_up_id trên sổ); giao dịch lỗi rollback hết.
    @Test
    void sameTopUpCannotCreditTwice() {
        Customer c = customer();
        UUID topUpId = seedCoins(c.userId(), 100);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> coinWallets.apply(coinWallets.lockUserWallet(c.userId()),
                CoinEntryType.TOP_UP, 50, null, em.getReference(com.vesanrebackend.entity.coin.CoinTopUp.class, topUpId))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(balance(c.userId())).isEqualTo(100);
    }

    // Kiểm tra: cùng một khách đặt đồng thời 2 sân (mỗi cái 30 coin) khi chỉ có 40 coin -> một 201, một 409.
    @Test
    void concurrentBookingsSameCustomerOnlyOneAffordable() throws Exception {
        BookableVenue bc = bookableVenue(1, 30);
        Customer c = customer();
        seedCoins(c.userId(), 40);
        java.time.LocalDate date = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.CountDownLatch go = new java.util.concurrent.CountDownLatch(1);
            List<java.util.concurrent.Future<ResponseEntity<Map>>> futures = new java.util.ArrayList<>();
            for (String[] t : new String[][]{{"18:00", "19:00"}, {"20:00", "21:00"}}) {
                futures.add(pool.submit(() -> {
                    go.await();
                    return post(client(), "/api/bookings", Map.of("venueId", bc.venueId().toString(),
                            "date", date.toString(), "startTime", t[0], "endTime", t[1]), c.token());
                }));
            }
            go.countDown();
            List<ResponseEntity<Map>> results = new java.util.ArrayList<>();
            for (var f : futures) results.add(f.get());
            assertThat(results).extracting(r -> r.getStatusCode().value()).containsExactlyInAnyOrder(201, 409);
            ResponseEntity<Map> loser = results.get(0).getStatusCode().value() == 409 ? results.get(0) : results.get(1);
            assertThat(loser.getBody().get("detail")).isEqualTo("Insufficient coins");
        } finally {
            pool.shutdownNow();
        }
        assertThat(balance(c.userId())).isEqualTo(10).isEqualTo(ledgerSum(c.userId()));
    }

    // Kiểm tra: không token -> 401.
    @Test
    void walletRequiresLogin() {
        assertThat(get(client(), "/api/wallet", null).getStatusCode().value()).isEqualTo(401);
    }
}
