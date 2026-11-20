package com.vesanrebackend.controller.review;

import com.vesanrebackend.BookingApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewIntegrationTest extends BookingApiTestSupport {

    private ResponseEntity<Map> review(Customer c, UUID bookingId, Integer rating, String comment) {
        Map<String, Object> body = new HashMap<>();
        body.put("rating", rating);
        body.put("comment", comment);
        return post(client(), "/api/bookings/" + bookingId + "/review", body, c.token());
    }

    private ResponseEntity<Map> review(Customer c, UUID bookingId, int rating) {
        return review(c, bookingId, rating, null);
    }

    @Test
    void onlyOwnCompletedBookingOnce() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        Customer other = customer();
        UUID confirmed = confirmedBooking(c, bc);
        assertThat(review(c, confirmed, 5).getStatusCode().value()).isEqualTo(409);
        UUID done = completedBooking(c, bc);
        assertThat(review(other, done, 5).getStatusCode().value()).isEqualTo(404);
        assertThat(review(c, UUID.randomUUID(), 5).getStatusCode().value()).isEqualTo(404);
        ResponseEntity<Map> ok = review(c, done, 4, "Sân đẹp");
        assertThat(ok.getStatusCode().value()).isEqualTo(201);
        assertThat(ok.getBody().get("rating")).isEqualTo(4);
        assertThat(ok.getBody().get("comment")).isEqualTo("Sân đẹp");
        assertThat(ok.getBody().get("customerName")).isEqualTo("IT Customer");
        assertThat(ok.getBody().get("status")).isEqualTo("PUBLISHED");
        assertThat(review(c, done, 5).getStatusCode().value()).isEqualTo(409);
        Map detail = get(client(), "/api/bookings/" + done, c.token()).getBody();
        assertThat(((Map) detail.get("review")).get("rating")).isEqualTo(4);
        assertThat(get(client(), "/api/bookings/" + confirmed, c.token()).getBody().get("review")).isNull();
    }

    @Test
    void notifiesShopOwner() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        UUID done = completedBooking(c, bc);
        assertThat(review(c, done, 5).getStatusCode().value()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sporthub.notifications WHERE user_id = ? "
                + "AND notification_type = 'REVIEW_CREATED' AND booking_id = ?", Long.class, bc.provider().userId(), done))
                .isEqualTo(1);
    }

    @Test
    void validatesInput() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        UUID done = completedBooking(c, bc);
        assertThat(review(c, done, 0).getStatusCode().value()).isEqualTo(400);
        assertThat(review(c, done, 6).getStatusCode().value()).isEqualTo(400);
        assertThat(review(c, done, null, null).getStatusCode().value()).isEqualTo(400);
        assertThat(review(c, done, 5, "x".repeat(1001)).getStatusCode().value()).isEqualTo(400);
        assertThat(review(c, done, 5, "x".repeat(1000)).getStatusCode().value()).isEqualTo(201);
    }

    private UUID reviewed(BookableCourt bc, int rating) {
        Customer c = customer();
        ResponseEntity<Map> r = review(c, completedBooking(c, bc), rating);
        assertThat(r.getStatusCode().value()).isEqualTo(201);
        return UUID.fromString((String) r.getBody().get("id"));
    }

    private ResponseEntity<Map> reply(String token, UUID reviewId, String text) {
        return post(client(), "/api/provider/reviews/" + reviewId + "/reply", Map.of("reply", text), token);
    }

    private List<Map<String, Object>> items(ResponseEntity<Map> page) {
        return (List<Map<String, Object>>) page.getBody().get("items");
    }

    @Test
    void publicListAndRatingAverage() {
        BookableCourt bc = bookableCourt();
        String venue = "/api/catalog/venues/" + bc.venueId();
        Map detail = get(client(), venue, null).getBody();
        assertThat(detail.get("ratingAverage")).isNull();
        assertThat(detail.get("ratingCount")).isEqualTo(0);
        reviewed(bc, 4);
        UUID newest = reviewed(bc, 5);
        detail = get(client(), venue, null).getBody();
        assertThat(new BigDecimal(detail.get("ratingAverage").toString())).isEqualByComparingTo("4.5");
        assertThat(detail.get("ratingCount")).isEqualTo(2);
        List<Map<String, Object>> list = items(get(client(), venue + "/reviews", null));
        assertThat(list).hasSize(2);
        assertThat(list.get(0).get("id")).isEqualTo(newest.toString());
        assertThat(list.get(0).get("customerName")).isEqualTo("IT Customer");
        reviewed(bc, 4);
        detail = get(client(), venue, null).getBody();
        assertThat(new BigDecimal(detail.get("ratingAverage").toString())).isEqualByComparingTo("4.3");
        assertThat(detail.get("ratingCount")).isEqualTo(3);
        assertThat(get(client(), "/api/catalog/venues/" + UUID.randomUUID() + "/reviews", null)
                .getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void providerRepliesOnceOwnShopOnly() {
        BookableCourt bc = bookableCourt();
        BookableCourt other = bookableCourt();
        UUID r = reviewed(bc, 3);
        assertThat(reply(other.provider().token(), r, "Hi").getStatusCode().value()).isEqualTo(404);
        assertThat(items(get(client(), "/api/provider/reviews?size=100", other.provider().token())))
                .noneMatch(i -> r.toString().equals(((Map) i.get("review")).get("id")));
        List<Map<String, Object>> own = items(get(client(), "/api/provider/reviews?size=100", bc.provider().token()));
        assertThat(own).hasSize(1);
        assertThat(own.get(0).get("venueName")).isNotNull();
        assertThat(own.get(0).get("bookingCode")).isNotNull();
        ResponseEntity<Map> ok = reply(bc.provider().token(), r, "Cảm ơn");
        assertThat(ok.getStatusCode().value()).isEqualTo(200);
        assertThat(ok.getBody().get("providerReply")).isEqualTo("Cảm ơn");
        assertThat(ok.getBody().get("repliedAt")).isNotNull();
        assertThat(reply(bc.provider().token(), r, "Lần 2").getStatusCode().value()).isEqualTo(409);
        assertThat(reply(bc.provider().token(), r, "").getStatusCode().value()).isEqualTo(400);
        assertThat(items(get(client(), "/api/catalog/venues/" + bc.venueId() + "/reviews", null)).get(0)
                .get("providerReply")).isEqualTo("Cảm ơn");
    }

    @Test
    void wrongRoleForbidden() {
        assertThat(get(client(), "/api/provider/reviews", customer().token()).getStatusCode().value())
                .isEqualTo(403);
    }
}
