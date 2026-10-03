package com.vesanrebackend.controller.provider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared base for the provider catalog API tests (Tasks 3-7 add their tests here). Real Tomcat + PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProviderCatalogIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    record Provider(UUID userId, String token) {
    }

    @Test
    void catalogIsPublicAndListsSeededSportsAndAmenities() {
        RestClient client = client();
        ResponseEntity<List> sports = client.get().uri("/api/catalog/sports").retrieve().toEntity(List.class);
        assertThat(sports.getStatusCode().value()).isEqualTo(200);
        assertThat((List<Map<String, Object>>) sports.getBody()).anySatisfy(s ->
                assertThat(s).containsEntry("code", "BADMINTON").containsEntry("name", "Cầu lông")
                        .containsEntry("id", sportId("BADMINTON").toString()));

        ResponseEntity<List> amenities = client.get().uri("/api/catalog/amenities").retrieve().toEntity(List.class);
        assertThat(amenities.getStatusCode().value()).isEqualTo(200);
        assertThat((List<Map<String, Object>>) amenities.getBody()).anySatisfy(a ->
                assertThat(a).containsEntry("code", "LIGHTING").containsEntry("scope", "COURT")
                        .containsEntry("id", amenityId("LIGHTING").toString()));

        // A provider token reads it too (also exercises verifiedProvider()).
        Provider provider = verifiedProvider();
        assertThat(get(client, "/api/catalog/sports", provider.token(), List.class).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void shopReadUpdateAndChangeRequestFlow() {
        RestClient client = client();
        Provider provider = verifiedProvider();
        String token = provider.token();

        Map shop = get(client, "/api/provider/shop", token).getBody();
        String name = (String) shop.get("name");
        assertThat(shop.get("pendingChange")).isNull();

        // Free field applies immediately.
        assertThat(patch(client, "/api/provider/shop", Map.of("description", "Mo ta moi"), token).getBody())
                .containsEntry("description", "Mo ta moi");
        // Missing field = unchanged (PATCH {} keeps the description).
        assertThat(patch(client, "/api/provider/shop", Map.of(), token).getBody())
                .containsEntry("description", "Mo ta moi");

        // Same name as current -> nothing changed -> 400.
        assertThat(post(client, "/api/provider/shop/change-request", Map.of("name", "  " + name + " "), token)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(post(client, "/api/provider/shop/change-request", Map.of("name", "   "), token)
                .getStatusCode().value()).isEqualTo(400);

        // Important field: old name stays, pendingChange appears.
        ResponseEntity<Map> created = post(client, "/api/provider/shop/change-request", Map.of("name", "Ten moi"), token);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody()).containsEntry("name", name);
        Map pending = (Map) get(client, "/api/provider/shop", token).getBody().get("pendingChange");
        assertThat((Map) pending.get("proposed")).containsEntry("name", "Ten moi");
        assertThat(pending.get("createdAt")).isNotNull();

        // Second request while one is pending -> 409.
        assertThat(post(client, "/api/provider/shop/change-request", Map.of("name", "Ten khac"), token)
                .getStatusCode().value()).isEqualTo(409);

        // Review focus #5: another provider -> 404; owner cancel -> 204; cancel again -> 409.
        String requestId = (String) pending.get("id");
        assertThat(post(client, "/api/provider/change-requests/" + requestId + "/cancel", Map.of(), verifiedProvider().token())
                .getStatusCode().value()).isEqualTo(404);
        assertThat(post(client, "/api/provider/change-requests/" + requestId + "/cancel", Map.of(), token)
                .getStatusCode().value()).isEqualTo(204);
        assertThat(post(client, "/api/provider/change-requests/" + requestId + "/cancel", Map.of(), token)
                .getStatusCode().value()).isEqualTo(409);
        assertThat(get(client, "/api/provider/shop", token).getBody().get("pendingChange")).isNull();

        // Cancelled -> resubmit works.
        assertThat(post(client, "/api/provider/shop/change-request", Map.of("name", "Ten khac"), token)
                .getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void venueCreateSubmitDeleteAndOwnership() {
        RestClient client = client();
        String token = verifiedProvider().token();
        String other = verifiedProvider().token();

        Map<String, Object> body = venueBody("San A");
        assertThat(post(client, "/api/provider/venues", Map.of("name", "x"), token).getStatusCode().value()).isEqualTo(400);
        Map<String, Object> halfCoords = new java.util.HashMap<>(body);
        halfCoords.remove("longitude");
        assertThat(post(client, "/api/provider/venues", halfCoords, token).getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map> created = post(client, "/api/provider/venues", body, token);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        String id = (String) created.getBody().get("id");
        assertThat(created.getBody()).containsEntry("status", "DRAFT");
        assertThat((String) created.getBody().get("slug")).startsWith("san-a-");
        assertThat(get(client, "/api/provider/venues", token, List.class).getBody()).hasSize(1);

        // Spec #2 (venue part): another provider gets 404 on GET / DELETE / submit / amenities.
        assertThat(get(client, "/api/provider/venues/" + id, other).getStatusCode().value()).isEqualTo(404);
        assertThat(delete(client, "/api/provider/venues/" + id, other).getStatusCode().value()).isEqualTo(404);
        assertThat(post(client, "/api/provider/venues/" + id + "/submit", Map.of(), other).getStatusCode().value()).isEqualTo(404);
        assertThat(put(client, "/api/provider/venues/" + id + "/amenities", List.of(), other).getStatusCode().value()).isEqualTo(404);
        assertThat(get(client, "/api/provider/venues", other, List.class).getBody()).isEmpty();

        // Spec #1: submit without an ACTIVE court -> 409; with one -> PENDING_REVIEW, then no re-submit / no delete.
        assertThat(post(client, "/api/provider/venues/" + id + "/submit", Map.of(), token).getStatusCode().value()).isEqualTo(409);
        jdbc.update("INSERT INTO sporthub.courts (id, venue_id, code, name, capacity, booking_step_minutes, min_booking_minutes, max_booking_minutes)"
                + " VALUES (gen_random_uuid(), ?, 'C1', 'Court 1', 4, 30, 60, 120)", UUID.fromString(id));
        ResponseEntity<Map> submitted = post(client, "/api/provider/venues/" + id + "/submit", Map.of(), token);
        assertThat(submitted.getStatusCode().value()).isEqualTo(200);
        assertThat(submitted.getBody()).containsEntry("status", "PENDING_REVIEW");
        assertThat((List) submitted.getBody().get("courts")).hasSize(1);
        assertThat(post(client, "/api/provider/venues/" + id + "/submit", Map.of(), token).getStatusCode().value()).isEqualTo(409);
        assertThat(delete(client, "/api/provider/venues/" + id, token).getStatusCode().value()).isEqualTo(409);

        // DRAFT venue with a court: DELETE -> 204 (courts removed first), then 404.
        String draft = (String) post(client, "/api/provider/venues", venueBody("San B"), token).getBody().get("id");
        jdbc.update("INSERT INTO sporthub.courts (id, venue_id, code, name, capacity, booking_step_minutes, min_booking_minutes, max_booking_minutes)"
                + " VALUES (gen_random_uuid(), ?, 'C1', 'Court 1', 4, 30, 60, 120)", UUID.fromString(draft));
        assertThat(delete(client, "/api/provider/venues/" + draft, token).getStatusCode().value()).isEqualTo(204);
        assertThat(get(client, "/api/provider/venues/" + draft, token).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void venueAmenitiesReplaceAllAndScope() {
        RestClient client = client();
        String token = verifiedProvider().token();
        String id = (String) post(client, "/api/provider/venues", venueBody("San C"), token).getBody().get("id");
        String path = "/api/provider/venues/" + id + "/amenities";
        Map<String, Object> parking = Map.of("amenityId", amenityId("PARKING").toString(), "details", "50 slots");
        Map<String, Object> wifi = Map.of("amenityId", amenityId("WIFI").toString());

        // Review focus #4: COURT-only amenity on a venue -> 400; unknown id -> 400; duplicate -> 400.
        assertThat(put(client, path, List.of(Map.of("amenityId", amenityId("LIGHTING").toString())), token)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path, List.of(Map.of("amenityId", UUID.randomUUID().toString())), token)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path, List.of(parking, parking), token).getStatusCode().value()).isEqualTo(400);

        // Replace-all twice with the same key must succeed both times.
        assertThat(put(client, path, List.of(parking, wifi), token).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> second = put(client, path, List.of(parking), token);
        assertThat(second.getStatusCode().value()).isEqualTo(200);
        assertThat((List<Map>) second.getBody().get("amenities")).hasSize(1).first()
                .satisfies(a -> assertThat(a).containsEntry("code", "PARKING").containsEntry("details", "50 slots"));
        // A null element must be a 400, not an NPE/500.
        assertThat(put(client, path, java.util.Arrays.asList((Object) null), token).getStatusCode().value()).isEqualTo(400);
        // BOTH-scope amenity is fine on a venue; empty list clears.
        assertThat(put(client, path, List.of(Map.of("amenityId", amenityId("EQUIPMENT_RENTAL").toString())), token)
                .getStatusCode().value()).isEqualTo(200);
        assertThat((List) put(client, path, List.of(), token).getBody().get("amenities")).isEmpty();
        assertThat((List) get(client, "/api/provider/venues/" + id, token).getBody().get("amenities")).isEmpty();
    }

    @Test
    void venuePatchAndChangeRequestByStatus() {
        RestClient client = client();
        String token = verifiedProvider().token();
        String id = (String) post(client, "/api/provider/venues", venueBody("San D"), token).getBody().get("id");
        String path = "/api/provider/venues/" + id;

        // DRAFT: important fields apply immediately; optional "" clears; trimmed.
        ResponseEntity<Map> draft = patch(client, path, Map.of("name", "  San D2 ", "ward", "P1"), token);
        assertThat(draft.getStatusCode().value()).isEqualTo(200);
        assertThat(draft.getBody()).containsEntry("name", "San D2").containsEntry("ward", "P1");
        assertThat(patch(client, path, Map.of("ward", ""), token).getBody().get("ward")).isNull();
        assertThat(patch(client, path, Map.of("name", "  "), token).getStatusCode().value()).isEqualTo(400);
        assertThat(patch(client, path, Map.of("latitude", 11.0, "description", "x"), token).getBody())
                .containsEntry("latitude", 11.0).containsEntry("description", "x"); // pair stays valid: longitude kept
        // Change request is not for DRAFT.
        assertThat(post(client, path + "/change-request", Map.of("name", "Khac"), token).getStatusCode().value()).isEqualTo(409);

        // PENDING_REVIEW: important -> 409, free -> 200.
        jdbc.update("UPDATE sporthub.venues SET status = 'PENDING_REVIEW' WHERE id = ?", UUID.fromString(id));
        assertThat(patch(client, path, Map.of("name", "Khac"), token).getStatusCode().value()).isEqualTo(409);
        assertThat(patch(client, path, Map.of("phone", "0900"), token).getStatusCode().value()).isEqualTo(200);

        // REJECTED: important fields apply immediately again.
        jdbc.update("UPDATE sporthub.venues SET status = 'REJECTED' WHERE id = ?", UUID.fromString(id));
        ResponseEntity<Map> rejected = patch(client, path, Map.of("name", "San D3"), token);
        assertThat(rejected.getStatusCode().value()).isEqualTo(200);
        assertThat(rejected.getBody()).containsEntry("name", "San D3");
        patch(client, path, Map.of("name", "San D2"), token);

        // Spec #3: ACTIVE (set via JDBC).
        jdbc.update("UPDATE sporthub.venues SET status = 'ACTIVE' WHERE id = ?", UUID.fromString(id));
        assertThat(patch(client, path, Map.of("name", "Ten moi"), token).getStatusCode().value()).isEqualTo(409);
        // Mixed important + free PATCH is rejected as a whole: nothing partially applied.
        assertThat(patch(client, path, Map.of("name", "Ten moi", "description", "Mo ta khac"), token)
                .getStatusCode().value()).isEqualTo(409);
        assertThat(get(client, path, token).getBody()).containsEntry("description", "x");

        // Review focus #1: same important values re-sent (+ new description) -> 200.
        Map<String, Object> form = new java.util.HashMap<>(venueBody("San D2"));
        form.put("latitude", 11.0);
        form.put("description", "Mo ta");
        ResponseEntity<Map> resent = patch(client, path, form, token);
        assertThat(resent.getStatusCode().value()).isEqualTo(200);
        assertThat(resent.getBody()).containsEntry("description", "Mo ta");

        // Review focus #2: same coordinates (DB scale 6, request scale 1) -> unchanged -> 400 (nothing changed).
        assertThat(post(client, path + "/change-request", Map.of("latitude", 11.000000, "longitude", 106.700000, "name", " San D2 "), token)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(post(client, path + "/change-request", Map.of(), token).getStatusCode().value()).isEqualTo(400);

        // Change request keeps data, shows pendingChange; second -> 409; cancel then resubmit -> 201.
        ResponseEntity<Map> created = post(client, path + "/change-request", Map.of("name", "Ten moi", "city", "HN"), token);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody()).containsEntry("name", "San D2").containsEntry("city", "HCM");
        Map pending = (Map) created.getBody().get("pendingChange");
        assertThat((Map) pending.get("proposed")).containsEntry("name", "Ten moi").containsEntry("city", "HN")
                .doesNotContainKey("district");
        assertThat(get(client, path, token).getBody()).containsEntry("name", "San D2");
        assertThat(post(client, path + "/change-request", Map.of("name", "Ten khac"), token).getStatusCode().value()).isEqualTo(409);
        assertThat(post(client, "/api/provider/change-requests/" + pending.get("id") + "/cancel", Map.of(), token)
                .getStatusCode().value()).isEqualTo(204);
        assertThat(post(client, path + "/change-request", Map.of("name", "Ten khac"), token).getStatusCode().value()).isEqualTo(201);

        // SUSPENDED accepts change requests (cancel the pending one first).
        Map pendingNow = (Map) get(client, path, token).getBody().get("pendingChange");
        assertThat(post(client, "/api/provider/change-requests/" + pendingNow.get("id") + "/cancel", Map.of(), token)
                .getStatusCode().value()).isEqualTo(204);
        jdbc.update("UPDATE sporthub.venues SET status = 'SUSPENDED' WHERE id = ?", UUID.fromString(id));
        assertThat(post(client, path + "/change-request", Map.of("name", "Ten sau"), token).getStatusCode().value()).isEqualTo(201);

        // Another provider -> 404.
        String other = verifiedProvider().token();
        assertThat(patch(client, path, Map.of("phone", "1"), other).getStatusCode().value()).isEqualTo(404);
        assertThat(post(client, path + "/change-request", Map.of("name", "z"), other).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void courtCreateGetPatchAndOwnership() {
        RestClient client = client();
        String token = verifiedProvider().token();
        String other = verifiedProvider().token();
        String venueId = (String) post(client, "/api/provider/venues", venueBody("San E"), token).getBody().get("id");
        String courts = "/api/provider/venues/" + venueId + "/courts";
        Map<String, Object> body = Map.of("code", " C1 ", "name", "Court 1", "capacity", 4,
                "bookingStepMinutes", 30, "minBookingMinutes", 60, "maxBookingMinutes", 120);

        ResponseEntity<Map> created = post(client, courts, body, token);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody()).containsEntry("code", "C1").containsEntry("status", "ACTIVE")
                .containsEntry("venueId", venueId);
        String id = (String) created.getBody().get("id");
        String path = "/api/provider/courts/" + id;

        // GET returns the full shape; child lists are empty until the PUT endpoints fill them.
        Map detail = get(client, path, token).getBody();
        assertThat(detail).containsEntry("capacity", 4);
        for (String list : List.of("sports", "amenities", "operatingHours", "pricingRules")) {
            assertThat((List) detail.get(list)).isEmpty();
        }
        assertThat((List) get(client, "/api/provider/venues/" + venueId, token).getBody().get("courts")).hasSize(1);

        // Duplicate code in the same venue -> 409; same code in another venue is fine.
        assertThat(post(client, courts, body, token).getStatusCode().value()).isEqualTo(409);
        String venue2 = (String) post(client, "/api/provider/venues", venueBody("San F"), token).getBody().get("id");
        assertThat(post(client, "/api/provider/venues/" + venue2 + "/courts", body, token).getStatusCode().value()).isEqualTo(201);

        // Validation: min not a multiple of step, min > max, capacity 0, blank code -> 400.
        for (Map<String, Object> bad : List.<Map<String, Object>>of(
                merge(body, "code", "C2", "minBookingMinutes", 45),
                merge(body, "code", "C2", "minBookingMinutes", 150),
                merge(body, "code", "C2", "capacity", 0),
                merge(body, "code", "  "))) {
            assertThat(post(client, courts, bad, token).getStatusCode().value()).isEqualTo(400);
        }

        // PATCH: status, free fields; merged values re-validated (max 120 vs new step 50 -> 400, no 500).
        ResponseEntity<Map> inactive = patch(client, path, Map.of("status", "INACTIVE", "name", "Court A"), token);
        assertThat(inactive.getStatusCode().value()).isEqualTo(200);
        assertThat(inactive.getBody()).containsEntry("status", "INACTIVE").containsEntry("name", "Court A");
        assertThat(patch(client, path, Map.of("status", "DELETED"), token).getStatusCode().value()).isEqualTo(400);
        assertThat(patch(client, path, Map.of("bookingStepMinutes", 50), token).getStatusCode().value()).isEqualTo(400);
        assertThat(patch(client, path, Map.of("maxBookingMinutes", 30), token).getStatusCode().value()).isEqualTo(400);
        assertThat(patch(client, path, Map.of("minBookingMinutes", 120, "maxBookingMinutes", 240), token)
                .getStatusCode().value()).isEqualTo(200);
        assertThat(get(client, path, token).getBody()).containsEntry("minBookingMinutes", 120)
                .containsEntry("maxBookingMinutes", 240).containsEntry("status", "INACTIVE");

        // Another provider: 404 on create / GET / PATCH.
        assertThat(post(client, courts, merge(body, "code", "C9"), other).getStatusCode().value()).isEqualTo(404);
        assertThat(get(client, path, other).getStatusCode().value()).isEqualTo(404);
        assertThat(patch(client, path, Map.of("name", "x"), other).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void courtReplaceAllPutsReturnNewDataAndAreRepeatable() {
        RestClient client = client();
        String token = verifiedProvider().token();
        String other = verifiedProvider().token();
        String venueId = (String) post(client, "/api/provider/venues", venueBody("San G"), token).getBody().get("id");
        String id = (String) post(client, "/api/provider/venues/" + venueId + "/courts", Map.of("code", "C1", "name", "Court 1",
                "capacity", 4, "bookingStepMinutes", 30, "minBookingMinutes", 60, "maxBookingMinutes", 120), token)
                .getBody().get("id");
        String path = "/api/provider/courts/" + id;

        // Sports: empty / duplicate / 0 or 2 primary / unknown / null element -> 400; replace twice, response is fresh.
        Map<String, Object> badminton = Map.of("sportId", sportId("BADMINTON").toString(), "primary", true);
        Map<String, Object> tennis = Map.of("sportId", sportId("TENNIS").toString(), "primary", false);
        assertThat(put(client, path + "/sports", List.of(), token).getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/sports", List.of(badminton, badminton), token).getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/sports", List.of(tennis), token).getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/sports", List.of(badminton, merge(tennis, "primary", true)), token)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/sports", List.of(Map.of("sportId", UUID.randomUUID().toString(), "primary", true)), token)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/sports", java.util.Arrays.asList((Object) null), token).getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/sports", List.of(badminton, tennis), token).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> sports = put(client, path + "/sports", List.of(merge(tennis, "primary", true)), token);
        assertThat(sports.getStatusCode().value()).isEqualTo(200);
        assertThat((List<Map>) sports.getBody().get("sports")).hasSize(1).first()
                .satisfies(s -> assertThat(s).containsEntry("code", "TENNIS").containsEntry("primary", true));
        assertThat((List) get(client, path, token).getBody().get("sports")).hasSize(1);

        // Amenities: VENUE-scope (PARKING) -> 400 (Review Focus #4); COURT and BOTH ok; unknown/duplicate 400; replace twice.
        assertThat(put(client, path + "/amenities", List.of(Map.of("amenityId", amenityId("PARKING").toString())), token)
                .getStatusCode().value()).isEqualTo(400);
        Map<String, Object> lighting = Map.of("amenityId", amenityId("LIGHTING").toString(), "details", "LED");
        assertThat(put(client, path + "/amenities", List.of(Map.of("amenityId", UUID.randomUUID().toString())), token)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/amenities", List.of(lighting, lighting), token).getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/amenities", java.util.Arrays.asList((Object) null), token).getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/amenities", List.of(lighting, Map.of("amenityId", amenityId("EQUIPMENT_RENTAL").toString())), token)
                .getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> amenities = put(client, path + "/amenities", List.of(lighting), token);
        assertThat(amenities.getStatusCode().value()).isEqualTo(200);
        assertThat((List<Map>) amenities.getBody().get("amenities")).hasSize(1).first()
                .satisfies(a -> assertThat(a).containsEntry("code", "LIGHTING").containsEntry("details", "LED"));
        assertThat((List) get(client, path, token).getBody().get("amenities")).hasSize(1);

        // Operating hours: needs 7 distinct weekdays; closed => no times, else opens < closes.
        assertThat(put(client, path + "/operating-hours", hours(6, "06:00", "22:00"), token).getStatusCode().value()).isEqualTo(400);
        List<Map<String, Object>> dupDay = new java.util.ArrayList<>(hours(7, "06:00", "22:00"));
        dupDay.set(6, dupDay.get(0));
        assertThat(put(client, path + "/operating-hours", dupDay, token).getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/operating-hours", hours(7, "22:00", "06:00"), token).getStatusCode().value()).isEqualTo(400);
        List<Map<String, Object>> closedWithTimes = new java.util.ArrayList<>(hours(7, "06:00", "22:00"));
        closedWithTimes.set(6, Map.of("weekday", 6, "opensAt", "06:00", "closesAt", "22:00", "closed", true));
        assertThat(put(client, path + "/operating-hours", closedWithTimes, token).getStatusCode().value()).isEqualTo(400);
        List<Map<String, Object>> valid = new java.util.ArrayList<>(hours(7, "06:00", "22:00"));
        valid.set(6, Map.of("weekday", 6, "closed", true));
        assertThat(put(client, path + "/operating-hours", valid, token).getStatusCode().value()).isEqualTo(200);
        valid.set(0, Map.of("weekday", 0, "opensAt", "07:00", "closesAt", "21:00", "closed", false));
        ResponseEntity<Map> hoursResp = put(client, path + "/operating-hours", valid, token);
        assertThat(hoursResp.getStatusCode().value()).isEqualTo(200);
        List<Map> hourItems = (List<Map>) hoursResp.getBody().get("operatingHours");
        assertThat(hourItems).hasSize(7);
        assertThat(hourItems.get(0)).containsEntry("opensAt", "07:00:00").containsEntry("closed", false);
        assertThat(hourItems.get(6)).containsEntry("closed", true).containsEntry("opensAt", null);

        // Pricing: overlap in one request -> 400; start >= end / > 1440 / price <= 0 -> 400; touching ranges are fine.
        assertThat(put(client, path + "/pricing-rules", List.of(rule(0, 0, 600, 100000), rule(0, 599, 1440, 150000)), token)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/pricing-rules", List.of(rule(0, 600, 600, 100000)), token).getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/pricing-rules", List.of(rule(0, 0, 1441, 100000)), token).getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/pricing-rules", List.of(rule(0, 0, 600, 0)), token).getStatusCode().value()).isEqualTo(400);
        assertThat(put(client, path + "/pricing-rules", java.util.Arrays.asList((Object) null), token).getStatusCode().value()).isEqualTo(400);
        List<Map<String, Object>> rules = List.of(rule(0, 0, 600, 100000), rule(0, 600, 1440, 150000), rule(1, 0, 1440, 120000));
        // Review focus #3: same payload twice -> both 200.
        assertThat(put(client, path + "/pricing-rules", rules, token).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> pricing = put(client, path + "/pricing-rules", rules, token);
        assertThat(pricing.getStatusCode().value()).isEqualTo(200);
        assertThat((List<Map>) pricing.getBody().get("pricingRules")).hasSize(3).first()
                .satisfies(r -> assertThat(r).containsEntry("currency", "VND").containsEntry("startMinute", 0));
        // New data (not the previous rows) in the response and in a follow-up GET.
        ResponseEntity<Map> replaced = put(client, path + "/pricing-rules", List.of(rule(2, 60, 120, 99000)), token);
        assertThat((List<Map>) replaced.getBody().get("pricingRules")).hasSize(1).first()
                .satisfies(r -> assertThat(r).containsEntry("weekday", 2).containsEntry("startMinute", 60));
        assertThat((List<Map>) get(client, path, token).getBody().get("pricingRules")).hasSize(1);
        assertThat((List) put(client, path + "/pricing-rules", List.of(), token).getBody().get("pricingRules")).isEmpty();
        // The earlier PUTs were not clobbered by the later ones.
        Map detail = get(client, path, token).getBody();
        assertThat((List) detail.get("sports")).hasSize(1);
        assertThat((List) detail.get("operatingHours")).hasSize(7);

        // Another provider -> 404 on every PUT.
        assertThat(put(client, path + "/sports", List.of(badminton), other).getStatusCode().value()).isEqualTo(404);
        assertThat(put(client, path + "/amenities", List.of(), other).getStatusCode().value()).isEqualTo(404);
        assertThat(put(client, path + "/operating-hours", valid, other).getStatusCode().value()).isEqualTo(404);
        assertThat(put(client, path + "/pricing-rules", rules, other).getStatusCode().value()).isEqualTo(404);
    }

    // Spec #1 end to end through the real APIs: venue -> court -> hours -> prices -> sports -> submit.
    @Test
    void venueCourtHoursPricingSportsThenSubmitPendingReview() {
        RestClient client = client();
        String token = verifiedProvider().token();
        String venueId = (String) post(client, "/api/provider/venues", venueBody("San H"), token).getBody().get("id");
        String submit = "/api/provider/venues/" + venueId + "/submit";
        assertThat(post(client, submit, Map.of(), token).getStatusCode().value()).isEqualTo(409); // no court yet
        String courtId = (String) post(client, "/api/provider/venues/" + venueId + "/courts", Map.of("code", "C1", "name", "Court 1",
                "capacity", 4, "bookingStepMinutes", 30, "minBookingMinutes", 60, "maxBookingMinutes", 120), token)
                .getBody().get("id");
        String court = "/api/provider/courts/" + courtId;
        assertThat(put(client, court + "/operating-hours", hours(7, "06:00", "22:00"), token).getStatusCode().value()).isEqualTo(200);
        assertThat(put(client, court + "/pricing-rules", List.of(rule(0, 0, 1440, 100000)), token).getStatusCode().value()).isEqualTo(200);
        assertThat(put(client, court + "/sports", List.of(Map.of("sportId", sportId("BADMINTON").toString(), "primary", true)), token)
                .getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> submitted = post(client, submit, Map.of(), token);
        assertThat(submitted.getStatusCode().value()).isEqualTo(200);
        assertThat(submitted.getBody()).containsEntry("status", "PENDING_REVIEW");
    }

    private static List<Map<String, Object>> hours(int days, String opens, String closes) {
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (int d = 0; d < days; d++) {
            out.add(Map.of("weekday", d, "opensAt", opens, "closesAt", closes, "closed", false));
        }
        return out;
    }

    private static Map<String, Object> rule(int weekday, int start, int end, int price) {
        return Map.of("weekday", weekday, "startMinute", start, "endMinute", end, "pricePerHour", price);
    }

    private static Map<String, Object> merge(Map<String, Object> base, Object... overrides) {
        Map<String, Object> out = new java.util.HashMap<>(base);
        for (int i = 0; i < overrides.length; i += 2) {
            out.put((String) overrides[i], overrides[i + 1]);
        }
        return out;
    }

    Map<String, Object> venueBody(String name) {
        return Map.of("name", name, "addressLine", "1 Le Loi", "district", "Quan 1", "city", "HCM",
                "latitude", 10.5, "longitude", 106.7);
    }

    UUID sportId(String code) {
        return jdbc.queryForObject("SELECT id FROM sporthub.sports WHERE code = ?", UUID.class, code);
    }

    UUID amenityId(String code) {
        return jdbc.queryForObject("SELECT id FROM sporthub.amenities WHERE code = ?", UUID.class, code);
    }

    /** register -> apply -> JDBC verify + PROVIDER role + ACTIVE shop -> login (roles are read at login). */
    Provider verifiedProvider() {
        RestClient client = client();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "catalog-" + suffix + "@example.com";
        UUID userId = UUID.fromString((String) post(client, "/api/auth/register", Map.of("email", email,
                "password", "correct-password", "displayName", "IT Catalog " + suffix)).getBody().get("id"));
        String applyToken = login(client, email);
        assertThat(post(client, "/api/profile/provider-application", Map.of("legalName", "Catalog " + suffix), applyToken)
                .getStatusCode().value()).isEqualTo(201);
        jdbc.update("UPDATE sporthub.provider_profiles SET status = 'VERIFIED', verified_at = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
        jdbc.update("INSERT INTO sporthub.user_roles (user_id, role_code) VALUES (?, 'PROVIDER')", userId);
        jdbc.update("UPDATE sporthub.shops SET status = 'ACTIVE' WHERE owner_user_id = ?", userId);
        return new Provider(userId, login(client, email));
    }

    RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    String login(RestClient client, String email) {
        return (String) post(client, "/api/auth/login", Map.of("email", email, "password", "correct-password"))
                .getBody().get("accessToken");
    }

    ResponseEntity<Map> get(RestClient client, String path, String token) {
        return get(client, path, token, Map.class);
    }

    <T> ResponseEntity<T> get(RestClient client, String path, String token, Class<T> type) {
        return client.get().uri(path)
                .headers(headers -> { if (token != null) headers.setBearerAuth(token); })
                .retrieve().toEntity(type);
    }

    ResponseEntity<Map> post(RestClient client, String path, Map<String, Object> body) {
        return post(client, path, body, null);
    }

    ResponseEntity<Map> post(RestClient client, String path, Map<String, Object> body, String token) {
        return client.post().uri(path).headers(headers -> { if (token != null) headers.setBearerAuth(token); })
                .body(body).retrieve().toEntity(Map.class);
    }

    ResponseEntity<Map> patch(RestClient client, String path, Map<String, Object> body, String token) {
        return client.patch().uri(path).headers(headers -> headers.setBearerAuth(token)).body(body).retrieve().toEntity(Map.class);
    }

    ResponseEntity<Map> put(RestClient client, String path, Object body, String token) {
        return client.put().uri(path).headers(headers -> headers.setBearerAuth(token)).body(body).retrieve().toEntity(Map.class);
    }

    ResponseEntity<Map> delete(RestClient client, String path, String token) {
        return client.delete().uri(path).headers(headers -> headers.setBearerAuth(token)).retrieve().toEntity(Map.class);
    }
}
