package com.vesanrebackend.controller.catalog;

import com.vesanrebackend.AdminApiTestSupport;
import com.vesanrebackend.service.storage.ImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Loại test: integration HTTP thật + PostgreSQL (kế thừa AdminApiTestSupport); ImageStorage được mock.
 * API: public /api/catalog/venues|locations (Task sau thêm venues/{id}, courts/{id}/availability).
 * Mọi test tìm kiếm lọc theo city duy nhất vì DB test dùng chung.
 */
class PublicCatalogIntegrationTest extends AdminApiTestSupport {
    @MockitoBean
    private ImageStorage storage;

    @BeforeEach
    void fakeStorage() {
        when(storage.url(anyString())).thenAnswer(inv -> "https://img.test/" + inv.getArgument(0));
    }

    // API: GET /api/catalog/venues, GET /api/catalog/locations
    // Kiểm tra: chỉ venue ACTIVE xuất hiện; lọc district/sport/q (không phân biệt hoa thường, % và _ được escape); locations đúng.
    @Test
    void searchShowsOnlyActiveVenuesAndFilters() {
        RestClient client = client();
        Provider p = verifiedProvider();
        String city = uniqueCity();
        UUID a = activeVenue(p, "Alpha_1", city, "D1");
        sport(p.token(), court(p.token(), a, "C1"), "BADMINTON");
        UUID b = activeVenue(p, "Beta 100%", city, "D2");
        sport(p.token(), court(p.token(), b, "C1"), "TENNIS");
        UUID c = draftVenue(p, "Gamma", city, "D3");
        jdbc.update("UPDATE sporthub.venues SET status = 'PENDING_REVIEW' WHERE id = ?", c);
        UUID d = draftVenue(p, "Delta", city, "D3");
        jdbc.update("UPDATE sporthub.venues SET status = 'SUSPENDED' WHERE id = ?", d);
        draftVenue(p, "Epsilon", city, "D3");

        assertThat(ids(client, Map.of("city", city))).containsExactlyInAnyOrder(a.toString(), b.toString());
        assertThat(ids(client, Map.of("city", city, "district", "D1"))).containsExactly(a.toString());
        assertThat(ids(client, Map.of("city", city, "district", "D1", "sport", "TENNIS"))).isEmpty();
        assertThat(ids(client, Map.of("city", city, "sport", "TENNIS"))).containsExactly(b.toString());
        assertThat(ids(client, Map.of("city", city, "q", "ALPHA"))).containsExactly(a.toString());
        assertThat(ids(client, Map.of("city", city, "q", "%"))).containsExactly(b.toString());
        assertThat(ids(client, Map.of("city", city, "q", "_"))).containsExactly(a.toString());

        List<Map<String, Object>> locations = get(client, "/api/catalog/locations", null, List.class).getBody();
        assertThat(locations.stream().filter(l -> city.equals(l.get("city"))).toList())
                .hasSize(1).first().satisfies(l -> assertThat((List<String>) l.get("districts")).containsExactly("D1", "D2"));
    }

    // API: GET /api/catalog/venues
    // Kiểm tra: fromPrice bỏ qua court INACTIVE và rule không active; lọc/sắp theo giá, venue không giá nằm cuối; cover, sports, null đúng.
    @Test
    void priceFilterSortAndEmptyVenue() {
        RestClient client = client();
        Provider p = verifiedProvider();
        String city = uniqueCity();
        UUID v1 = activeVenue(p, "V1", city, "D1");
        UUID c1 = court(p.token(), v1, "C1");
        sport(p.token(), c1, "BADMINTON");
        prices(p.token(), c1, 360, 1320, 100000);
        UUID c2 = court(p.token(), v1, "C2");
        prices(p.token(), c2, 360, 1320, 50000);
        jdbc.update("UPDATE sporthub.courts SET status = \'INACTIVE\' WHERE id = ?", c2);
        String key = "cover-" + UUID.randomUUID();
        jdbc.update("INSERT INTO sporthub.venue_images (id, venue_id, storage_key, sort_order, is_cover) VALUES (gen_random_uuid(), ?, ?, 0, true)",
                v1, key);

        UUID v2 = activeVenue(p, "V2", city, "D1");
        UUID c3 = court(p.token(), v2, "C1");
        prices(p.token(), c3, 360, 720, 200000, 720, 1320, 150000);
        jdbc.update("UPDATE sporthub.court_pricing_rules SET is_active = false WHERE court_id = ? AND price_per_hour = 150000", c3);

        UUID v3 = activeVenue(p, "V3", city, "D1");
        UUID c4 = court(p.token(), v3, "C1");
        jdbc.update("UPDATE sporthub.courts SET status = \'INACTIVE\' WHERE id = ?", c4);

        assertThat(ids(client, Map.of("city", city, "sort", "price_asc")))
                .containsExactly(v1.toString(), v2.toString(), v3.toString());
        assertThat(ids(client, Map.of("city", city, "sort", "price_desc")))
                .containsExactly(v2.toString(), v1.toString(), v3.toString());
        Map<String, Object> min = search(client, Map.of("city", city, "minPrice", "150000"));
        assertThat(items(min)).extracting(i -> i.get("id")).containsExactly(v2.toString());
        assertThat(((Number) min.get("totalElements")).intValue()).isEqualTo(1);
        assertThat(ids(client, Map.of("city", city, "maxPrice", "150000"))).containsExactly(v1.toString());

        Map<UUID, Map<String, Object>> byId = new HashMap<>();
        items(search(client, Map.of("city", city))).forEach(i -> byId.put(UUID.fromString((String) i.get("id")), i));
        Map<String, Object> card1 = byId.get(v1);
        assertThat(new BigDecimal(String.valueOf(card1.get("fromPrice")))).isEqualByComparingTo("100000");
        assertThat(card1.get("coverImageUrl")).isEqualTo("https://img.test/" + key);
        assertThat((List<String>) card1.get("sports")).containsExactly("Cầu lông");
        Map<String, Object> card3 = byId.get(v3);
        assertThat((List<String>) card3.get("sports")).isEmpty();
        assertThat(card3.get("fromPrice")).isNull();
        assertThat(card3.get("coverImageUrl")).isNull();
        assertThat(card3.get("ratingAverage")).isNull();
        assertThat(card3.get("ratingCount")).isEqualTo(0);
    }

    // API: GET /api/catalog/venues
    // Kiểm tra: sort lạ, minPrice > maxPrice, giá âm, q quá dài, page âm, minPrice không phải số đều trả 400.
    @Test
    void invalidSearchParamsAre400() {
        RestClient client = client();
        List<Map<String, String>> bad = List.of(Map.of("sort", "foo"), Map.of("minPrice", "5", "maxPrice", "1"),
                Map.of("minPrice", "-1"), Map.of("q", "x".repeat(101)), Map.of("page", "-1"), Map.of("minPrice", "abc"));
        for (Map<String, String> params : bad) {
            assertThat(rawSearch(client, params).getStatusCode().value()).as(params.toString()).isEqualTo(400);
        }
    }

    // API: GET /api/catalog/venues/{id}
    // Kiểm tra: chi tiết chỉ có court ACTIVE, rule active, ảnh/logo/tiện ích/giờ; không lộ status, slug, pendingChange.
    @Test
    void detailShowsOnlyPublicData() {
        RestClient client = client();
        Provider p = verifiedProvider();
        UUID venue = activeVenue(p, "Detail", uniqueCity(), "D1");
        UUID c1 = court(p.token(), venue, "C1");
        sport(p.token(), c1, "BADMINTON");
        hours(p.token(), c1, "06:00", "22:00", null);
        prices(p.token(), c1, 360, 720, 100000, 720, 1320, 80000);
        jdbc.update("UPDATE sporthub.court_pricing_rules SET is_active = false WHERE court_id = ? AND price_per_hour = 80000", c1);
        UUID c2 = court(p.token(), venue, "C2");
        jdbc.update("UPDATE sporthub.courts SET status = \'INACTIVE\' WHERE id = ?", c2);
        assertThat(put(client, "/api/provider/venues/" + venue + "/amenities",
                List.of(Map.of("amenityId", jdbc.queryForObject("SELECT id FROM sporthub.amenities WHERE code = 'PARKING'", UUID.class).toString())),
                p.token()).getStatusCode().value()).isEqualTo(200);
        String key = "cover-" + UUID.randomUUID();
        jdbc.update("INSERT INTO sporthub.venue_images (id, venue_id, storage_key, sort_order, is_cover) VALUES (gen_random_uuid(), ?, ?, 0, true)",
                venue, key);
        jdbc.update("UPDATE sporthub.shops SET logo_storage_key = 'logo-k' WHERE owner_user_id = ?", p.userId());

        ResponseEntity<Map> response = get(client, "/api/catalog/venues/" + venue, null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body).doesNotContainKey("courts");
        assertThat(body.get("courtCount")).isEqualTo(1);
        assertThat(body.get("sport")).isEqualTo("Cầu lông");
        assertThat(body.get("depositPercent")).isNotNull();
        assertThat((List<Map<String, Object>>) body.get("pricingRules")).hasSize(7)
                .allSatisfy(r -> assertThat(new BigDecimal(String.valueOf(r.get("pricePerHour")))).isEqualByComparingTo("100000"));
        assertThat(((List<Map<String, Object>>) body.get("amenities")).get(0).get("code")).isEqualTo("PARKING");
        assertThat(((List<Map<String, Object>>) body.get("images")).get(0).get("url")).isEqualTo("https://img.test/" + key);
        assertThat(((Map<String, Object>) body.get("shop")).get("logoUrl")).isEqualTo("https://img.test/logo-k");
        assertThat(new BigDecimal(String.valueOf(body.get("fromPrice")))).isEqualByComparingTo("100000");
        assertThat(body).doesNotContainKeys("status", "slug", "pendingChange");
    }

    // API: GET /api/catalog/venues/{id}
    // Kiểm tra: venue DRAFT, PENDING_REVIEW, SUSPENDED và UUID ngẫu nhiên đều trả 404.
    @Test
    void nonPublicVenueDetailIs404() {
        RestClient client = client();
        Provider p = verifiedProvider();
        String city = uniqueCity();
        UUID draft = draftVenue(p, "Draft", city, "D1");
        UUID pending = draftVenue(p, "Pending", city, "D1");
        jdbc.update("UPDATE sporthub.venues SET status = 'PENDING_REVIEW' WHERE id = ?", pending);
        UUID suspended = draftVenue(p, "Suspended", city, "D1");
        jdbc.update("UPDATE sporthub.venues SET status = 'SUSPENDED' WHERE id = ?", suspended);
        for (UUID id : List.of(draft, pending, suspended, UUID.randomUUID())) {
            assertThat(get(client, "/api/catalog/venues/" + id, null).getStatusCode().value()).as(id.toString()).isEqualTo(404);
        }
    }

    // API: GET /api/catalog/venues/{id}/availability
    // Kiểm tra: venue 1 sân; block ACTIVE làm freeCourts=0, block RELEASED không ảnh hưởng, ngày closed trả closed=true và slots rỗng.
    @Test
    void availabilityMarksBlocksAndClosedDays() {
        RestClient client = client();
        Provider p = verifiedProvider();
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate d1 = LocalDate.now(zone).plusDays(1);
        LocalDate d2 = d1.plusDays(1);
        UUID venue = activeVenue(p, "Avail", uniqueCity(), "D1");
        UUID court = court(p.token(), venue, "C1");
        hours(p.token(), court, "06:00", "22:00", d2.getDayOfWeek().getValue() - 1);
        prices(p.token(), court, 0, 1440, 100000);
        jdbc.update("INSERT INTO sporthub.court_schedule_blocks (id, court_id, block_kind, state, start_at, end_at, created_by, reason)"
                        + " VALUES (gen_random_uuid(), ?, 'MAINTENANCE', 'ACTIVE', ?, ?, ?, 'IT')", court,
                instant(d1, 8, zone), instant(d1, 9, zone), p.userId());
        jdbc.update("INSERT INTO sporthub.court_schedule_blocks (id, court_id, block_kind, state, start_at, end_at, created_by, reason, released_at)"
                        + " VALUES (gen_random_uuid(), ?, 'MAINTENANCE', 'RELEASED', ?, ?, ?, 'IT', now())", court,
                instant(d1, 10, zone), instant(d1, 11, zone), p.userId());

        Map<String, Object> day1 = get(client, "/api/catalog/venues/" + venue + "/availability?date=" + d1, null).getBody();
        assertThat(day1.get("closed")).isEqualTo(false);
        assertThat(day1.get("courtCount")).isEqualTo(1);
        List<Map<String, Object>> slots = (List<Map<String, Object>>) day1.get("slots");
        assertThat(slots).hasSize(16);
        Map<String, Object> blocked = slots.stream().filter(s -> ((String) s.get("start")).startsWith("08:00")).findFirst().orElseThrow();
        Map<String, Object> released = slots.stream().filter(s -> ((String) s.get("start")).startsWith("10:00")).findFirst().orElseThrow();
        assertThat(blocked.get("freeCourts")).isEqualTo(0);
        assertThat(released.get("freeCourts")).isEqualTo(1);
        assertThat(new BigDecimal(String.valueOf(released.get("price")))).isEqualByComparingTo("100000");

        Map<String, Object> day2 = get(client, "/api/catalog/venues/" + venue + "/availability?date=" + d2, null).getBody();
        assertThat(day2.get("closed")).isEqualTo(true);
        assertThat((List<?>) day2.get("slots")).isEmpty();
    }

    // API: GET /api/catalog/venues/{id}/availability
    // Kiểm tra: venue không có sân ACTIVE, venue SUSPENDED, UUID lạ trả 404; date ngoài [hôm nay, +30], sai định dạng, thiếu trả 400; +30 trả 200.
    @Test
    void availabilityRejectsNonPublicVenueAndBadDates() {
        RestClient client = client();
        Provider p = verifiedProvider();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        UUID ok = activeVenue(p, "Avail2", uniqueCity(), "D1");
        UUID c1 = court(p.token(), ok, "S1");
        hours(p.token(), c1, "06:00", "22:00", null);
        prices(p.token(), c1, 0, 1440, 100000);
        UUID noCourts = activeVenue(p, "Avail2b", uniqueCity(), "D1");
        UUID inactiveOnly = activeVenue(p, "Avail2c", uniqueCity(), "D1");
        jdbc.update("UPDATE sporthub.courts SET status = 'INACTIVE' WHERE id = ?", court(p.token(), inactiveOnly, "S1"));
        UUID suspended = activeVenue(p, "Avail3", uniqueCity(), "D1");
        court(p.token(), suspended, "S1");
        jdbc.update("UPDATE sporthub.venues SET status = 'SUSPENDED' WHERE id = ?", suspended);

        for (UUID id : List.of(noCourts, inactiveOnly, suspended, UUID.randomUUID())) {
            assertThat(get(client, "/api/catalog/venues/" + id + "/availability?date=" + today, null).getStatusCode().value())
                    .as(id.toString()).isEqualTo(404);
        }
        String base = "/api/catalog/venues/" + ok + "/availability";
        for (String query : List.of("?date=" + today.plusDays(31), "?date=" + today.minusDays(1), "?date=abc", "")) {
            assertThat(get(client, base + query, null).getStatusCode().value()).as(query).isEqualTo(400);
        }
        assertThat(get(client, base + "?date=" + today.plusDays(30), null).getStatusCode().value()).isEqualTo(200);
    }

    // API: GET /api/catalog/venues/{id}/availability
    // Kiểm tra: venue 2 sân, 1 block 18-19 -> mốc 18 freeCourts=1; mốc 17 freeCourts2h=1 (chỉ sân còn lại rảnh cả 17 và 18).
    @Test
    void venueAvailabilityCountsBookedCourts() {
        Provider p = verifiedProvider();
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate d = LocalDate.now(zone).plusDays(1);
        UUID venue = activeVenue(p, "Avail4", uniqueCity(), "D1");
        UUID s1 = court(p.token(), venue, "S1");
        UUID s2 = court(p.token(), venue, "S2");
        for (UUID c : List.of(s1, s2)) {
            hours(p.token(), c, "06:00", "22:00", null);
            prices(p.token(), c, 0, 1440, 100000);
        }
        jdbc.update("INSERT INTO sporthub.court_schedule_blocks (id, court_id, block_kind, state, start_at, end_at, created_by, reason)"
                        + " VALUES (gen_random_uuid(), ?, 'MAINTENANCE', 'ACTIVE', ?, ?, ?, 'IT')", s1,
                instant(d, 18, zone), instant(d, 19, zone), p.userId());

        Map<String, Object> body = get(client(), "/api/catalog/venues/" + venue + "/availability?date=" + d, null).getBody();
        assertThat(body.get("courtCount")).isEqualTo(2);
        List<Map<String, Object>> slots = (List<Map<String, Object>>) body.get("slots");
        Map<String, Object> at17 = slots.stream().filter(s -> ((String) s.get("start")).startsWith("17:00")).findFirst().orElseThrow();
        Map<String, Object> at18 = slots.stream().filter(s -> ((String) s.get("start")).startsWith("18:00")).findFirst().orElseThrow();
        assertThat(at18.get("freeCourts")).isEqualTo(1);
        assertThat(at17.get("freeCourts")).isEqualTo(2);
        assertThat(at17.get("freeCourts2h")).isEqualTo(1);
    }

    // ---- helpers (Task 3, 4 dùng lại) ----

    // Timestamp, not LocalDateTime: a LocalDateTime would be read in the JVM timezone.
    Timestamp instant(LocalDate date, int hour, ZoneId zone) {
        return Timestamp.from(date.atTime(hour, 0).atZone(zone).toInstant());
    }

    String uniqueCity() {
        return "IT-" + UUID.randomUUID().toString().substring(0, 8);
    }

    UUID draftVenue(Provider p, String name, String city, String district) {
        Map<String, Object> body = new HashMap<>(venueBody(name));
        body.put("city", city);
        body.put("district", district);
        ResponseEntity<Map> created = post(client(), "/api/provider/venues", body, p.token());
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        return UUID.fromString((String) created.getBody().get("id"));
    }

    UUID activeVenue(Provider p, String name, String city, String district) {
        UUID id = draftVenue(p, name, city, district);
        jdbc.update("UPDATE sporthub.venues SET status = 'ACTIVE' WHERE id = ?", id);
        return id;
    }

    // The court API is gone (courts are hidden, set up per venue): these helpers seed court rows directly.
    UUID court(String token, UUID venueId, String code) {
        return jdbc.queryForObject("INSERT INTO sporthub.courts (id, venue_id, code, name, capacity, booking_step_minutes, "
                + "min_booking_minutes, max_booking_minutes) VALUES (gen_random_uuid(), ?, ?, ?, 4, 60, 60, 120) RETURNING id",
                UUID.class, venueId, code, "Court " + code);
    }

    void sport(String token, UUID courtId, String code) {
        jdbc.update("INSERT INTO sporthub.court_sports (court_id, sport_id, is_primary) VALUES (?, ?, true)", courtId,
                sportId(code));
    }

    // triples of (startMinute, endMinute, pricePerHour), repeated for weekdays 0-6
    void prices(String token, UUID courtId, int... startEndPrice) {
        for (int weekday = 0; weekday < 7; weekday++) {
            for (int i = 0; i < startEndPrice.length; i += 3) {
                jdbc.update("INSERT INTO sporthub.court_pricing_rules (id, court_id, weekday, start_minute, end_minute, "
                                + "price_per_hour, currency) VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, 'VND')", courtId,
                        weekday, startEndPrice[i], startEndPrice[i + 1], startEndPrice[i + 2]);
            }
        }
    }

    // closedWeekday (nullable) is stored as closed with no hours
    void hours(String token, UUID courtId, String opens, String closes, Integer closedWeekday) {
        for (int weekday = 0; weekday < 7; weekday++) {
            boolean closed = closedWeekday != null && closedWeekday == weekday;
            jdbc.update("INSERT INTO sporthub.court_operating_hours (court_id, weekday, opens_at, closes_at, is_closed) "
                            + "VALUES (?, ?, ?, ?, ?)", courtId, weekday, closed ? null : java.time.LocalTime.parse(opens),
                    closed ? null : java.time.LocalTime.parse(closes), closed);
        }
    }

    UUID sportId(String code) {
        return jdbc.queryForObject("SELECT id FROM sporthub.sports WHERE code = ?", UUID.class, code);
    }

    <T> ResponseEntity<T> get(RestClient client, String path, String token, Class<T> type) {
        return client.get().uri(path)
                .headers(headers -> { if (token != null) headers.setBearerAuth(token); })
                .retrieve().toEntity(type);
    }

    // Values go through URI variables so '%' and '_' are encoded, not parsed.
    ResponseEntity<Map> rawSearch(RestClient client, Map<String, String> params) {
        StringBuilder uri = new StringBuilder("/api/catalog/venues?size=100");
        params.keySet().forEach(k -> uri.append('&').append(k).append("={").append(k).append('}'));
        return client.get().uri(uri.toString(), params).retrieve().toEntity(Map.class);
    }

    Map<String, Object> search(RestClient client, Map<String, String> params) {
        ResponseEntity<Map> response = rawSearch(client, params);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return response.getBody();
    }

    List<Map<String, Object>> items(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("items");
    }

    List<String> ids(RestClient client, Map<String, String> params) {
        return items(search(client, params)).stream().map(i -> (String) i.get("id")).toList();
    }
}
