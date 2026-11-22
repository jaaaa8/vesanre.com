package com.vesanrebackend.service.report;

import com.vesanrebackend.dto.report.FinanceReport;
import com.vesanrebackend.dto.report.MonthlyBookings;
import com.vesanrebackend.dto.report.TopShop;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.repository.ShopRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Aggregates computed on read (no summary tables); month boundaries are Vietnam time. */
@Service
public class ReportService {
    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    // Shared by the provider report and top-shops: only CONFIRMED/COMPLETED count, by the block's VN month.
    private static final String COUNTED_BOOKINGS = " FROM sporthub.bookings b"
            + " JOIN sporthub.court_schedule_blocks s ON s.id = b.schedule_block_id"
            + " JOIN sporthub.courts c ON c.id = s.court_id"
            + " JOIN sporthub.venues v ON v.id = c.venue_id"
            + " JOIN sporthub.shops sh ON sh.id = v.shop_id"
            + " WHERE b.status IN ('CONFIRMED', 'COMPLETED') AND s.start_at >= ? AND s.start_at < ?";

    private final JdbcTemplate jdbc;
    private final ShopRepository shops;
    private final Clock clock;

    public ReportService(JdbcTemplate jdbc, ShopRepository shops, Clock clock) {
        this.jdbc = jdbc;
        this.shops = shops;
        this.clock = clock;
    }

    static Instant startOf(YearMonth m) {
        return m.atDay(1).atStartOfDay(VN).toInstant();
    }

    // pgjdbc does not bind Instant.
    private static OffsetDateTime param(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    YearMonth currentMonth() {
        return YearMonth.now(clock.withZone(VN));
    }

    @Transactional(readOnly = true)
    public List<MonthlyBookings> providerMonthly(UUID ownerUserId, int months) {
        if (months < 1 || months > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "months must be between 1 and 24");
        }
        Shop shop = shops.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
        YearMonth current = currentMonth();
        YearMonth from = current.minusMonths(months); // one extra month for the first row's change
        Map<String, Long> counts = new HashMap<>();
        jdbc.query("SELECT to_char(s.start_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') AS month, count(*) AS n"
                        + COUNTED_BOOKINGS + " AND sh.id = ? GROUP BY 1",
                rs -> {
                    counts.put(rs.getString("month"), rs.getLong("n"));
                },
                param(startOf(from)), param(startOf(current.plusMonths(1))), shop.getId());
        List<MonthlyBookings> rows = new ArrayList<>();
        long prev = counts.getOrDefault(from.toString(), 0L);
        for (YearMonth m = from.plusMonths(1); !m.isAfter(current); m = m.plusMonths(1)) {
            long cur = counts.getOrDefault(m.toString(), 0L);
            BigDecimal percent = prev == 0 ? null
                    : BigDecimal.valueOf((cur - prev) * 100).divide(BigDecimal.valueOf(prev), 1, RoundingMode.HALF_UP);
            rows.add(new MonthlyBookings(m.toString(), cur, cur - prev, percent));
            prev = cur;
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<TopShop> topShops(YearMonth month, int limit) {
        if (limit < 1 || limit > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 50");
        }
        YearMonth m = month == null ? currentMonth() : month;
        return jdbc.query("SELECT sh.id, sh.name, count(*) AS n" + COUNTED_BOOKINGS
                        + " GROUP BY sh.id, sh.name ORDER BY count(*) DESC, sh.name LIMIT ?",
                (rs, i) -> new TopShop(rs.getObject("id", UUID.class), rs.getString("name"), rs.getLong("n")),
                param(startOf(m)), param(startOf(m.plusMonths(1))), limit);
    }

    @Transactional(readOnly = true)
    public FinanceReport finance(YearMonth month) {
        YearMonth m = month == null ? currentMonth() : month;
        OffsetDateTime from = param(startOf(m));
        OffsetDateTime to = param(startOf(m.plusMonths(1)));
        BigDecimal topUp = jdbc.queryForObject("SELECT COALESCE(SUM(amount_vnd), 0) FROM sporthub.coin_top_ups"
                + " WHERE status = 'PAID' AND paid_at >= ? AND paid_at < ?", BigDecimal.class, from, to);
        Map<String, Object> paid = jdbc.queryForMap("SELECT COALESCE(SUM(fee_coins), 0) * 1000 AS fee,"
                + " COALESCE(SUM(payout_vnd), 0) AS payout FROM sporthub.shop_withdrawals"
                + " WHERE status = 'PAID' AND paid_at >= ? AND paid_at < ?", from, to);
        Long wallets = jdbc.queryForObject(
                "SELECT COALESCE(SUM(balance), 0) FROM sporthub.coin_wallets WHERE shop_id IS NOT NULL", Long.class);
        Long pending = jdbc.queryForObject(
                "SELECT COALESCE(SUM(coin_amount), 0) FROM sporthub.shop_withdrawals WHERE status = 'PENDING'", Long.class);
        return new FinanceReport(m.toString(), topUp, new BigDecimal(paid.get("fee").toString()),
                new BigDecimal(paid.get("payout").toString()), wallets, pending);
    }
}
