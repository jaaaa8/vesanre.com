package com.vesanrebackend.controller.provider;

import com.vesanrebackend.AdminApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loại test: integration HTTP + PostgreSQL thật cho PUT/GET /api/provider/venues/{id}/setup - đồng bộ N sân ẩn
 * S1..Sn theo cấu hình cấp venue (bật/tắt sân, chặn thu hẹp khi có booking tương lai, giờ tròn, quyền sở hữu).
 */
@SuppressWarnings("unchecked")
class VenueSetupIntegrationTest extends AdminApiTestSupport {
    private String newVenue(RestClient client, Provider p) {
        return (String) post(client, "/api/provider/venues", venueBody("Setup " + UUID.randomUUID()), p.token())
                .getBody().get("id");
    }

    private int putSetup(RestClient client, Provider p, String venueId, Map<String, Object> body) {
        return put(client, "/api/provider/venues/" + venueId + "/setup", body, p.token()).getStatusCode().value();
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private Map<String, Object> with(Map<String, Object> base, String key, Object value) {
        Map<String, Object> m = new HashMap<>(base);
        m.put(key, value);
        return m;
    }

    // Kiểm tra: PUT courtCount=3, % cọc 20 -> 3 sân ACTIVE S1..S3, mỗi sân đủ 7 dòng giờ, bảng giá, môn primary; GET trả lại đúng.
    @Test
    void setupCreatesCourtsAndCopiesConfig() {
        RestClient client = client();
        Provider p = verifiedProvider();
        String venueId = newVenue(client, p);
        var response = put(client, "/api/provider/venues/" + venueId + "/setup", setupBody(3, 20), p.token());
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        assertThat(jdbc.queryForList("SELECT code FROM sporthub.courts WHERE venue_id = ?::uuid AND status = 'ACTIVE' ORDER BY code",
                String.class, venueId)).containsExactly("S1", "S2", "S3");
        assertThat(count("SELECT count(*) FROM sporthub.court_operating_hours h JOIN sporthub.courts c ON c.id = h.court_id "
                + "WHERE c.venue_id = ?::uuid", venueId)).isEqualTo(21);
        assertThat(count("SELECT count(*) FROM sporthub.court_pricing_rules r JOIN sporthub.courts c ON c.id = r.court_id "
                + "WHERE c.venue_id = ?::uuid AND r.price_per_hour = 100000", venueId)).isEqualTo(21);
        assertThat(count("SELECT count(*) FROM sporthub.court_sports cs JOIN sporthub.courts c ON c.id = cs.court_id "
                + "JOIN sporthub.sports s ON s.id = cs.sport_id WHERE c.venue_id = ?::uuid AND s.code = 'BADMINTON' "
                + "AND cs.is_primary", venueId)).isEqualTo(3);
        assertThat(count("SELECT deposit_percent FROM sporthub.venues WHERE id = ?::uuid", venueId)).isEqualTo(20);

        Map<String, Object> got = (Map<String, Object>) get(client, "/api/provider/venues/" + venueId + "/setup", p.token()).getBody();
        assertThat(got).containsEntry("sportCode", "BADMINTON").containsEntry("courtCount", 3)
                .containsEntry("depositPercent", 20);
        assertThat((List<?>) got.get("operatingHours")).hasSize(7);
        assertThat((List<?>) got.get("pricingRules")).hasSize(7);
        assertThat((Map<String, Object>) ((List<?>) got.get("operatingHours")).get(0)).containsEntry("opensAt", "06:00:00")
                .containsEntry("closed", false);
    }

    // Kiểm tra: thu hẹp 3 -> 2 tắt S3; mở lại 3 bật lại S3, không sinh S4.
    @Test
    void shrinkDeactivatesHighestFreeCourts() {
        RestClient client = client();
        Provider p = verifiedProvider();
        String venueId = newVenue(client, p);
        assertThat(putSetup(client, p, venueId, setupBody(3, 30))).isEqualTo(200);
        assertThat(putSetup(client, p, venueId, setupBody(2, 30))).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT status FROM sporthub.courts WHERE venue_id = ?::uuid AND code = 'S3'",
                String.class, venueId)).isEqualTo("INACTIVE");
        assertThat(putSetup(client, p, venueId, setupBody(3, 30))).isEqualTo(200);
        assertThat(jdbc.queryForList("SELECT code || ':' || status FROM sporthub.courts WHERE venue_id = ?::uuid ORDER BY code",
                String.class, venueId)).containsExactly("S1:ACTIVE", "S2:ACTIVE", "S3:ACTIVE");
    }

    // Kiểm tra: S1 và S2 đều có booking ngày mai -> bớt xuống 1 sân trả 409 và cả hai vẫn ACTIVE.
    @Test
    void shrinkBlockedByFutureBooking() {
        RestClient client = client();
        Provider p = verifiedProvider();
        String venueId = newVenue(client, p);
        assertThat(putSetup(client, p, venueId, setupBody(1, 30))).isEqualTo(200);
        blockTomorrow(venueId, "S1", p);
        assertThat(putSetup(client, p, venueId, setupBody(2, 30))).isEqualTo(200);
        blockTomorrow(venueId, "S2", p);
        assertThat(putSetup(client, p, venueId, setupBody(1, 30))).isEqualTo(409);
        assertThat(count("SELECT count(*) FROM sporthub.courts WHERE venue_id = ?::uuid AND status = 'ACTIVE'", venueId))
                .isEqualTo(2);
    }

    private void blockTomorrow(String venueId, String code, Provider p) {
        UUID courtId = jdbc.queryForObject("SELECT id FROM sporthub.courts WHERE venue_id = ?::uuid AND code = ?",
                UUID.class, venueId, code);
        jdbc.update("INSERT INTO sporthub.court_schedule_blocks (id, court_id, block_kind, start_at, end_at, created_by) "
                        + "VALUES (gen_random_uuid(), ?, 'BOOKING', now() + interval '1 day', now() + interval '1 day 1 hour', ?)",
                courtId, p.userId());
    }

    // Kiểm tra: giờ mở không tròn (06:30), mốc giá không chia hết 60 (90), môn không tồn tại -> 400.
    @Test
    void rejectsNonWholeHour() {
        RestClient client = client();
        Provider p = verifiedProvider();
        String venueId = newVenue(client, p);
        List<Map<String, Object>> hours = new ArrayList<>((List<Map<String, Object>>) setupBody(1, 30).get("operatingHours"));
        hours.set(0, Map.of("weekday", 0, "closed", false, "opensAt", "06:30", "closesAt", "22:00"));
        assertThat(putSetup(client, p, venueId, with(setupBody(1, 30), "operatingHours", hours))).isEqualTo(400);
        List<Map<String, Object>> rules = new ArrayList<>((List<Map<String, Object>>) setupBody(1, 30).get("pricingRules"));
        rules.set(0, Map.of("weekday", 0, "startMinute", 90, "endMinute", 1440, "pricePerHour", 100000));
        assertThat(putSetup(client, p, venueId, with(setupBody(1, 30), "pricingRules", rules))).isEqualTo(400);
        assertThat(putSetup(client, p, venueId, with(setupBody(1, 30), "sportCode", "NOPE"))).isEqualTo(400);
    }

    // Kiểm tra: provider khác không thấy venue (GET và PUT đều 404).
    @Test
    void otherProviderGets404() {
        RestClient client = client();
        Provider owner = verifiedProvider();
        Provider other = verifiedProvider();
        String venueId = newVenue(client, owner);
        assertThat(putSetup(client, other, venueId, setupBody(1, 30))).isEqualTo(404);
        assertThat(get(client, "/api/provider/venues/" + venueId + "/setup", other.token()).getStatusCode().value())
                .isEqualTo(404);
    }

    // Kiểm tra: API tạo sân cũ đã bị gỡ.
    @Test
    void oldCourtEndpointsGone() {
        RestClient client = client();
        Provider p = verifiedProvider();
        String venueId = newVenue(client, p);
        int status = post(client, "/api/provider/venues/" + venueId + "/courts", Map.of("code", "C1"), p.token())
                .getStatusCode().value();
        assertThat(status).isIn(404, 405);
    }
}
