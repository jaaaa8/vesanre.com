package com.vesanrebackend.controller.review;

import com.vesanrebackend.BookingApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
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
}
