package com.vesanrebackend.controller.catalog;

import com.vesanrebackend.AdminApiTestSupport;
import com.vesanrebackend.service.storage.ImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
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
        assertThat(patch(client, "/api/provider/courts/" + c2, Map.of("status", "INACTIVE"), p.token())
                .getStatusCode().value()).isEqualTo(200);
        String key = "cover-" + UUID.randomUUID();
        jdbc.update("INSERT INTO sporthub.venue_images (id, venue_id, storage_key, sort_order, is_cover) VALUES (gen_random_uuid(), ?, ?, 0, true)",
                v1, key);

        UUID v2 = activeVenue(p, "V2", city, "D1");
        UUID c3 = court(p.token(), v2, "C1");
        prices(p.token(), c3, 360, 720, 200000, 720, 1320, 150000);
        jdbc.update("UPDATE sporthub.court_pricing_rules SET is_active = false WHERE court_id = ? AND price_per_hour = 150000", c3);

        UUID v3 = activeVenue(p, "V3", city, "D1");
        UUID c4 = court(p.token(), v3, "C1");
        assertThat(patch(client, "/api/provider/courts/" + c4, Map.of("status", "INACTIVE"), p.token())
                .getStatusCode().value()).isEqualTo(200);

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

    // ---- helpers (Task 3, 4 dùng lại) ----

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

    UUID court(String token, UUID venueId, String code) {
        ResponseEntity<Map> created = post(client(), "/api/provider/venues/" + venueId + "/courts", Map.of("code", code,
                "name", "Court " + code, "capacity", 4, "bookingStepMinutes", 60, "minBookingMinutes", 60,
                "maxBookingMinutes", 120), token);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        return UUID.fromString((String) created.getBody().get("id"));
    }

    void sport(String token, UUID courtId, String code) {
        assertThat(put(client(), "/api/provider/courts/" + courtId + "/sports",
                List.of(Map.of("sportId", sportId(code).toString(), "primary", true)), token).getStatusCode().value())
                .isEqualTo(200);
    }

    // triples of (startMinute, endMinute, pricePerHour), repeated for weekdays 0-6
    void prices(String token, UUID courtId, int... startEndPrice) {
        List<Map<String, Object>> rules = new ArrayList<>();
        for (int weekday = 0; weekday < 7; weekday++) {
            for (int i = 0; i < startEndPrice.length; i += 3) {
                rules.add(Map.of("weekday", weekday, "startMinute", startEndPrice[i], "endMinute", startEndPrice[i + 1],
                        "pricePerHour", startEndPrice[i + 2]));
            }
        }
        assertThat(put(client(), "/api/provider/courts/" + courtId + "/pricing-rules", rules, token)
                .getStatusCode().value()).isEqualTo(200);
    }

    // closedWeekday (nullable) is sent as closed with no hours
    void hours(String token, UUID courtId, String opens, String closes, Integer closedWeekday) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int weekday = 0; weekday < 7; weekday++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("weekday", weekday);
            boolean closed = closedWeekday != null && closedWeekday == weekday;
            item.put("closed", closed);
            if (!closed) {
                item.put("opensAt", opens);
                item.put("closesAt", closes);
            }
            items.add(item);
        }
        assertThat(put(client(), "/api/provider/courts/" + courtId + "/operating-hours", items, token)
                .getStatusCode().value()).isEqualTo(200);
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
