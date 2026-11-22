package com.vesanrebackend.controller.notification;

import com.vesanrebackend.BookingApiTestSupport;
import com.vesanrebackend.service.notification.OutboxProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loại test: integration HTTP thật + PostgreSQL (kế thừa BookingApiTestSupport).
 * API: GET /api/notifications, POST /api/notifications/{id}/read; outbox processor gọi trực tiếp.
 */
class NotificationIntegrationTest extends BookingApiTestSupport {
    @Autowired
    private OutboxProcessor processor;

    private void drainOutbox() {
        int guard = 0;
        while (processor.processOne() && ++guard < 10000) {
            // keep draining, including events left over from earlier runs
        }
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    // Books 18:00-19:00 tomorrow then cancels; returns the booking id.
    private String bookAndCancel(BookableCourt bc, Customer c) {
        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        ResponseEntity<Map> r = post(client(), "/api/bookings", Map.of("courtId", bc.courtId().toString(),
                "date", tomorrow.toString(), "startTime", "18:00", "endTime", "19:00"), c.token());
        assertThat(r.getStatusCode().value()).isEqualTo(201);
        String id = (String) r.getBody().get("id");
        assertThat(post(client(), "/api/bookings/" + id + "/cancel", Map.of(), c.token()).getStatusCode().value())
                .isEqualTo(200);
        return id;
    }

    private long notifications(UUID userId) {
        return count("SELECT count(*) FROM sporthub.notifications WHERE user_id = ?", userId);
    }

    // Kiểm tra (spec case 8): CONFIRMED + CANCELLED báo cho khách và chủ shop, mail một lần, xử lý lại không nhân đôi.
    @Test
    void confirmedAndCancelledNotifyCustomerAndOwner() throws Exception {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        seedCoins(c.userId(), 100);
        String id = bookAndCancel(bc, c);
        drainOutbox();
        assertThat(count("SELECT count(*) FROM sporthub.notifications WHERE user_id = ? AND booking_id = ?::uuid "
                + "AND notification_type IN ('BOOKING_CONFIRMED','BOOKING_CANCELLED')", c.userId(), id)).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM sporthub.notifications WHERE user_id = ? AND booking_id = ?::uuid "
                + "AND notification_type IN ('BOOKING_CONFIRMED','BOOKING_CANCELLED')", bc.provider().userId(), id))
                .isEqualTo(2);
        assertThat(count("SELECT count(*) FROM sporthub.outbox_events WHERE aggregate_id = ?::uuid "
                + "AND status = 'PUBLISHED'", id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT body FROM sporthub.notifications WHERE user_id = ? "
                + "AND notification_type = 'BOOKING_CANCELLED'", String.class, c.userId())).contains("30 coin");
        awaitMails(c.email(), 2);

        drainOutbox();
        jdbc.update("UPDATE sporthub.outbox_events SET status = 'PENDING', published_at = NULL "
                + "WHERE event_key = ?", id + ":CONFIRMED");
        drainOutbox();
        Thread.sleep(300);
        assertThat(notifications(c.userId())).isEqualTo(2);
        assertThat(notifications(bc.provider().userId())).isEqualTo(2);
        assertThat(mailsTo(c.email())).hasSize(2);
    }

    // Kiểm tra: danh sách, unreadCount, đánh dấu đã đọc, lọc unreadOnly, người khác đọc -> 404.
    @Test
    @SuppressWarnings("unchecked")
    void notificationApi() {
        BookableCourt bc = bookableCourt();
        Customer c = customer();
        Customer other = customer();
        seedCoins(c.userId(), 100);
        bookAndCancel(bc, c);
        drainOutbox();
        Map<String, Object> page = get(client(), "/api/notifications", c.token()).getBody();
        assertThat(((Number) page.get("unreadCount")).longValue()).isEqualTo(2);
        List<Map<String, Object>> items = (List<Map<String, Object>>) page.get("items");
        assertThat(items).hasSize(2);
        String nid = (String) items.get(0).get("id");
        assertThat(post(client(), "/api/notifications/" + nid + "/read", Map.of(), other.token())
                .getStatusCode().value()).isEqualTo(404);
        assertThat(post(client(), "/api/notifications/" + nid + "/read", Map.of(), c.token())
                .getStatusCode().value()).isEqualTo(204);
        Map<String, Object> after = get(client(), "/api/notifications", c.token()).getBody();
        assertThat(((Number) after.get("unreadCount")).longValue()).isEqualTo(1);
        Map<String, Object> unread = get(client(), "/api/notifications?unreadOnly=true", c.token()).getBody();
        assertThat((List<?>) unread.get("items")).hasSize(1);
    }

    // Kiểm tra: event lỗi thử lại với backoff, đủ 5 lần thì FAILED.
    @Test
    void failingEventRetriesThenFails() {
        UUID aggregate = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO sporthub.outbox_events (id, event_key, aggregate_type, aggregate_id, event_type, "
                + "payload, next_attempt_at) VALUES (gen_random_uuid(), ?, 'BOOKING', ?, 'BOOKING_CANCELLED', "
                + "?::jsonb, now() - interval '1 minute')", key, aggregate, "{\"bookingId\":\"" + aggregate + "\"}");
        Instant before = Instant.now();
        drainOutbox();
        Map<String, Object> row = jdbc.queryForMap("SELECT status, attempt_count, next_attempt_at "
                + "FROM sporthub.outbox_events WHERE event_key = ?", key);
        assertThat(row.get("status")).isEqualTo("RETRY_PENDING");
        assertThat(((Number) row.get("attempt_count")).intValue()).isEqualTo(1);
        Instant next = ((java.sql.Timestamp) row.get("next_attempt_at")).toInstant();
        assertThat(next).isBetween(before.plusSeconds(55), Instant.now().plusSeconds(65));
        for (int i = 0; i < 4; i++) {
            jdbc.update("UPDATE sporthub.outbox_events SET next_attempt_at = now() - interval '1 second' "
                    + "WHERE event_key = ?", key);
            drainOutbox();
        }
        row = jdbc.queryForMap("SELECT status, attempt_count FROM sporthub.outbox_events WHERE event_key = ?", key);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(((Number) row.get("attempt_count")).intValue()).isEqualTo(5);
    }
}
