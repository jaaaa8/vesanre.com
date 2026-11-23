package com.vesanrebackend;

import com.vesanrebackend.entity.booking.Booking;
import com.vesanrebackend.entity.coin.CoinTopUp;
import com.vesanrebackend.entity.enums.CoinEntryType;
import com.vesanrebackend.service.coin.CoinWalletService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lớp nền cho integration test đặt sân và ví coin (kế thừa AdminApiTestSupport): thêm đồng hồ chỉnh được
 * (MutableClock thay bean Clock), helper tạo customer, sân đặt được, nạp coin trực tiếp và đọc số dư bằng JDBC.
 * Không chứa test.
 */
@Import(BookingApiTestSupport.ClockTestConfig.class)
public abstract class BookingApiTestSupport extends AdminApiTestSupport {
    /** Clock that follows real time until set(); zone UTC. */
    public static class MutableClock extends Clock {
        private volatile Instant fixed;

        public void set(Instant instant) {
            this.fixed = instant;
        }

        public void reset() {
            this.fixed = null;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new ZonedView(this, zone);
        }

        @Override
        public Instant instant() {
            Instant f = fixed;
            return f != null ? f : Instant.now();
        }
    }

    // withZone must keep following this clock, so wrap instead of freezing.
    private static final class ZonedView extends Clock {
        private final Clock base;
        private final ZoneId zone;

        ZonedView(Clock base, ZoneId zone) {
            this.base = base;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId z) {
            return new ZonedView(base, z);
        }

        @Override
        public Instant instant() {
            return base.instant();
        }
    }

    // Named testClock (not clock): Boot forbids overriding ClockConfig.clock; @Primary wins injection.
    @TestConfiguration
    static class ClockTestConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    protected record Customer(UUID userId, String email, String token) {
    }

    protected record BookableCourt(Provider provider, UUID venueId, UUID courtId, UUID shopId) {
    }

    // static: JUnit makes a new instance per test method, and slots must not repeat on a court.
    private static final AtomicInteger NEXT_SLOT = new AtomicInteger();

    @Autowired
    protected MutableClock testClock;

    @Autowired
    protected CoinWalletService coinWallets;

    @Autowired
    protected TransactionTemplate tx;

    @Autowired
    protected EntityManager em;

    @BeforeEach
    void resetClock() {
        testClock.reset();
    }

    protected Customer customer() {
        RestClient client = client();
        String email = "customer-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        UUID id = UUID.fromString((String) post(client, "/api/auth/register", Map.of("email", email,
                "password", "correct-password", "displayName", "IT Customer")).getBody().get("id"));
        return new Customer(id, email, login(client, email));
    }

    /** ACTIVE venue + ACTIVE court (step 60, min 60, max 120), open 06:00-22:00 every day, 100000/h all day. */
    protected BookableCourt bookableCourt() {
        RestClient client = client();
        Provider p = verifiedProvider();
        String venueId = (String) post(client, "/api/provider/venues", venueBody("Bookable " + UUID.randomUUID()),
                p.token()).getBody().get("id");
        jdbc.update("UPDATE sporthub.venues SET status = 'ACTIVE' WHERE id = ?::uuid", venueId);
        Map<?, ?> court = post(client, "/api/provider/venues/" + venueId + "/courts", Map.of("code", "C1",
                "name", "Court 1", "capacity", 4, "bookingStepMinutes", 60, "minBookingMinutes", 60,
                "maxBookingMinutes", 120), p.token()).getBody();
        String courtId = (String) court.get("id");
        List<Map<String, Object>> hours = new ArrayList<>();
        List<Map<String, Object>> rules = new ArrayList<>();
        for (int weekday = 0; weekday < 7; weekday++) {
            hours.add(Map.of("weekday", weekday, "closed", false, "opensAt", "06:00", "closesAt", "22:00"));
            rules.add(Map.of("weekday", weekday, "startMinute", 0, "endMinute", 1440, "pricePerHour", 100000));
        }
        assertThat(put(client, "/api/provider/courts/" + courtId + "/operating-hours", hours, p.token())
                .getStatusCode().value()).isEqualTo(200);
        assertThat(put(client, "/api/provider/courts/" + courtId + "/pricing-rules", rules, p.token())
                .getStatusCode().value()).isEqualTo(200);
        UUID shopId = jdbc.queryForObject("SELECT id FROM sporthub.shops WHERE owner_user_id = ?", UUID.class, p.userId());
        return new BookableCourt(p, UUID.fromString(venueId), UUID.fromString(courtId), shopId);
    }

    /** Credits coins through the real service, backed by a PAID top-up row; returns the top-up id. */
    protected UUID seedCoins(UUID userId, long coins) {
        return tx.execute(status -> {
            var wallet = coinWallets.lockUserWallet(userId);
            UUID topUpId = UUID.randomUUID();
            jdbc.update("INSERT INTO sporthub.coin_top_ups (id, wallet_id, transfer_code, coin_amount, amount_vnd, status, "
                            + "provider_code, provider_transaction_id, expires_at, paid_at) "
                            + "VALUES (?, ?, ?, ?, ?, 'PAID', 'TEST', ?, now() + interval '1 day', now())",
                    topUpId, wallet.getId(), "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                    coins, coins * 1000, UUID.randomUUID().toString());
            coinWallets.apply(wallet, CoinEntryType.TOP_UP, coins, null, em.getReference(CoinTopUp.class, topUpId));
            return topUpId;
        });
    }

    protected long balance(UUID userId) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(balance), 0) FROM sporthub.coin_wallets WHERE user_id = ?",
                Long.class, userId);
    }

    protected long ledgerSum(UUID userId) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(e.amount), 0) FROM sporthub.coin_ledger_entries e "
                + "JOIN sporthub.coin_wallets w ON w.id = e.wallet_id WHERE w.user_id = ?", Long.class, userId);
    }

    /** Seeds 100 coins and books 1h on the next unused slot (date +2.. days, 06:00-21:00 VN); returns the id. */
    protected UUID confirmedBooking(Customer c, BookableCourt bc) {
        seedCoins(c.userId(), 100);
        int n = NEXT_SLOT.getAndIncrement();
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2 + n / 16);
        int hour = 6 + n % 16;
        var response = post(client(), "/api/bookings", Map.of("courtId", bc.courtId().toString(),
                "date", date.toString(), "startTime", String.format("%02d:00", hour),
                "endTime", String.format("%02d:00", hour + 1)), c.token());
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    protected UUID completedBooking(Customer c, BookableCourt bc) {
        UUID id = confirmedBooking(c, bc);
        jdbc.update("UPDATE sporthub.bookings SET status = 'COMPLETED', completed_at = now() WHERE id = ?", id);
        return id;
    }

    /** Credits a shop wallet through a COMPLETED booking's BOOKING_PAYOUT (a CONFIRMED one would jam BookingCompletionJob). */
    protected void seedShopCoins(BookableCourt bc, long coins) {
        UUID bookingId = completedBooking(customer(), bc);
        tx.executeWithoutResult(status -> coinWallets.apply(coinWallets.lockShopWallet(bc.shopId()),
                CoinEntryType.BOOKING_PAYOUT, coins, em.getReference(Booking.class, bookingId), null));
    }

    protected long shopBalance(UUID shopId) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(balance), 0) FROM sporthub.coin_wallets WHERE shop_id = ?",
                Long.class, shopId);
    }

    protected long shopLedgerSum(UUID shopId) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(e.amount), 0) FROM sporthub.coin_ledger_entries e "
                + "JOIN sporthub.coin_wallets w ON w.id = e.wallet_id WHERE w.shop_id = ?", Long.class, shopId);
    }

    @SuppressWarnings("unchecked")
    protected List<Map> getList(String path, String token) {
        return client().get().uri(path).headers(h -> h.setBearerAuth(token)).retrieve().toEntity(List.class).getBody();
    }
}
