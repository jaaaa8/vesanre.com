package com.vesanrebackend;

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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /** ACTIVE venue + ACTIVE court (step 30, min 60, max 180), open 06:00-22:00 every day, 100000/h all day. */
    protected BookableCourt bookableCourt() {
        RestClient client = client();
        Provider p = verifiedProvider();
        String venueId = (String) post(client, "/api/provider/venues", venueBody("Bookable " + UUID.randomUUID()),
                p.token()).getBody().get("id");
        jdbc.update("UPDATE sporthub.venues SET status = 'ACTIVE' WHERE id = ?::uuid", venueId);
        Map<?, ?> court = post(client, "/api/provider/venues/" + venueId + "/courts", Map.of("code", "C1",
                "name", "Court 1", "capacity", 4, "bookingStepMinutes", 30, "minBookingMinutes", 60,
                "maxBookingMinutes", 180), p.token()).getBody();
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
}
