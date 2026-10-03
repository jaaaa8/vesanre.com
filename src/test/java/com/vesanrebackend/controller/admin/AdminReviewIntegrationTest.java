package com.vesanrebackend.controller.admin;

import com.vesanrebackend.AdminApiTestSupport;
import com.vesanrebackend.service.mail.ReviewMailer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AdminReviewIntegrationTest extends AdminApiTestSupport {
    @MockitoSpyBean
    private ReviewMailer mailer;

    @Test
    void venueFullCycleWithAuditAndMails() throws Exception {
        RestClient client = client();
        Admin admin = admin();
        Provider provider = verifiedProvider();
        UUID venueId = pendingVenue(provider.token(), "San Review");
        String base = "/api/admin/venues/" + venueId;

        // list summary + detail with court details
        assertThat(allItems(client, "/api/admin/venues", admin.token()))
                .anySatisfy(v -> assertThat(v).containsEntry("id", venueId.toString()).containsEntry("status", "PENDING_REVIEW"));
        Map detail = get(client, base, admin.token()).getBody();
        assertThat((Map<String, Object>) detail.get("venue")).containsEntry("name", "San Review");
        assertThat((List<?>) detail.get("courts")).hasSize(1);

        // reject (mail has reason) -> provider resubmits -> approve -> suspend -> reactivate
        assertThat(post(client, base + "/reject", Map.of("reason", "Thiếu ảnh"), admin.token()).getBody())
                .containsEntry("status", "REJECTED");
        assertThat(awaitMails(provider.email(), 1).get(0)).satisfies(m -> {
            assertThat(m.getSubject()).isEqualTo("[Vesanre] Địa điểm San Review bị từ chối");
            assertThat(m.getText()).contains("Lý do: Thiếu ảnh");
        });
        assertThat(post(client, "/api/provider/venues/" + venueId + "/submit", Map.of(), provider.token())
                .getStatusCode().value()).isEqualTo(200);
        assertThat(post(client, base + "/approve", Map.of(), admin.token()).getBody()).containsEntry("status", "ACTIVE");
        assertThat(post(client, base + "/suspend", Map.of("reason", "Khiếu nại"), admin.token()).getBody())
                .containsEntry("status", "SUSPENDED");
        assertThat(post(client, base + "/reactivate", Map.of(), admin.token()).getBody()).containsEntry("status", "ACTIVE");
        // @Async delivery: order is not guaranteed
        assertThat(awaitMails(provider.email(), 4)).extracting(SimpleMailMessage::getSubject).containsExactlyInAnyOrder(
                "[Vesanre] Địa điểm San Review bị từ chối", "[Vesanre] Địa điểm San Review đã được duyệt",
                "[Vesanre] Địa điểm San Review bị tạm đình chỉ", "[Vesanre] Địa điểm San Review đã được khôi phục");

        assertThat(jdbc.queryForList("SELECT action FROM sporthub.audit_logs WHERE entity_type = 'VENUE' AND entity_id = ? ORDER BY id",
                String.class, venueId)).containsExactly("VENUE_REJECTED", "VENUE_APPROVED", "VENUE_SUSPENDED", "VENUE_REACTIVATED");
        assertThat(jdbc.queryForObject("SELECT after_data->>'reason' FROM sporthub.audit_logs WHERE entity_id = ? AND action = 'VENUE_SUSPENDED'",
                String.class, venueId)).isEqualTo("Khiếu nại");

        // wrong source state -> 409 (and no extra mail), unknown -> 404
        assertThat(post(client, base + "/approve", Map.of(), admin.token()).getStatusCode().value()).isEqualTo(409);
        UUID pending = pendingVenue(provider.token(), "San Khac");
        assertThat(post(client, "/api/admin/venues/" + pending + "/suspend", Map.of("reason", "x"), admin.token())
                .getStatusCode().value()).isEqualTo(409);
        assertThat(post(client, "/api/admin/venues/" + UUID.randomUUID() + "/approve", Map.of(), admin.token())
                .getStatusCode().value()).isEqualTo(404);
        // send() runs inside the request, so counting calls needs no waiting: the 409/404 paths never reached the mailer.
        verify(mailer, times(4)).send(eq(provider.email()), any(), any(), any());
    }

    @Test
    void venueListIsPagedOldestFirst() {
        RestClient client = client();
        Admin admin = admin();
        String token = verifiedProvider().token();
        List<UUID> created = List.of(pendingVenue(token, "Page A"), pendingVenue(token, "Page B"), pendingVenue(token, "Page C"));

        Map page = get(client, "/api/admin/venues?size=2", admin.token()).getBody();
        long total = ((Number) page.get("totalElements")).longValue();
        assertThat((List<?>) page.get("items")).hasSize(2);
        assertThat(total).isGreaterThanOrEqualTo(3);
        assertThat(((Number) page.get("totalPages")).longValue()).isEqualTo((total + 1) / 2);
        assertThat(get(client, "/api/admin/venues?size=1000", admin.token()).getBody()).containsEntry("size", 100);

        List<String> ids = allItems(client, "/api/admin/venues", admin.token()).stream().map(v -> (String) v.get("id")).toList();
        assertThat(ids).containsSubsequence(created.stream().map(UUID::toString).toList()); // oldest first
    }

    @Autowired
    private DataSource dataSource;

    // Venue goes ACTIVE through the real admin API; returns the pending change request id.
    private String activeVenueWithChange(Admin admin, Provider provider, UUID venueId, Map<String, Object> change) {
        RestClient client = client();
        assertThat(post(client, "/api/admin/venues/" + venueId + "/approve", Map.of(), admin.token()).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> response = post(client, "/api/provider/venues/" + venueId + "/change-request", change, provider.token());
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return (String) ((Map) response.getBody().get("pendingChange")).get("id");
    }

    @Test
    void venueChangeRequestApproveAppliesExactCoordinates() throws Exception {
        RestClient client = client();
        Admin admin = admin();
        Provider provider = verifiedProvider();
        UUID venueId = pendingVenue(provider.token(), "San CR");
        String requestId = activeVenueWithChange(admin, provider, venueId,
                Map.of("name", "San CR Moi", "latitude", new BigDecimal("10.123456"), "longitude", new BigDecimal("106.654321")));

        Map<String, Object> listed = allItems(client, "/api/admin/change-requests?targetType=VENUE", admin.token()).stream()
                .filter(r -> requestId.equals(r.get("id"))).findFirst().orElseThrow();
        assertThat((Map<String, Object>) listed.get("current")).containsOnlyKeys("name", "latitude", "longitude")
                .containsEntry("name", "San CR");
        assertThat(listed).containsEntry("targetName", "San CR").containsEntry("submittedBy", provider.email());
        assertThat(allItems(client, "/api/admin/change-requests?targetType=SHOP", admin.token()))
                .noneSatisfy(r -> assertThat(r.get("id")).isEqualTo(requestId));

        assertThat(post(client, "/api/admin/change-requests/" + requestId + "/approve", Map.of(), admin.token())
                .getStatusCode().value()).isEqualTo(200);
        Map<String, Object> venue = jdbc.queryForMap("SELECT name, latitude, longitude, status FROM sporthub.venues WHERE id = ?", venueId);
        assertThat(venue).containsEntry("name", "San CR Moi").containsEntry("status", "ACTIVE");
        assertThat((BigDecimal) venue.get("latitude")).isEqualByComparingTo("10.123456");
        assertThat((BigDecimal) venue.get("longitude")).isEqualByComparingTo("106.654321");
        assertThat(jdbc.queryForMap("SELECT status, reviewed_by FROM sporthub.catalog_change_requests WHERE id = ?::uuid", requestId))
                .containsEntry("status", "APPROVED").containsEntry("reviewed_by", admin.userId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sporthub.audit_logs WHERE entity_type = 'CATALOG_CHANGE_REQUEST' "
                + "AND entity_id = ?::uuid AND action = 'CHANGE_REQUEST_APPROVED'", Integer.class, requestId)).isEqualTo(1);
        // @Async delivery: order is not guaranteed
        assertThat(awaitMails(provider.email(), 2)).extracting(SimpleMailMessage::getSubject)
                .contains("[Vesanre] Yêu cầu thay đổi San CR đã được duyệt");

        assertThat(post(client, "/api/admin/change-requests/" + requestId + "/approve", Map.of(), admin.token())
                .getStatusCode().value()).isEqualTo(409);
        assertThat(post(client, "/api/admin/change-requests/" + UUID.randomUUID() + "/approve", Map.of(), admin.token())
                .getStatusCode().value()).isEqualTo(404);
        // a new request is accepted once the old one is no longer PENDING
        assertThat(post(client, "/api/provider/venues/" + venueId + "/change-request", Map.of("name", "San CR 3"), provider.token())
                .getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void rejectKeepsDataStoresReasonAndMails() throws Exception {
        RestClient client = client();
        Admin admin = admin();
        Provider provider = verifiedProvider();
        UUID venueId = pendingVenue(provider.token(), "San Tu Choi");
        String requestId = activeVenueWithChange(admin, provider, venueId, Map.of("name", "Ten Bi Tu Choi"));

        assertThat(post(client, "/api/admin/change-requests/" + requestId + "/reject", Map.of("reason", "Tên không hợp lệ"), admin.token())
                .getStatusCode().value()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT name FROM sporthub.venues WHERE id = ?", String.class, venueId)).isEqualTo("San Tu Choi");
        assertThat(jdbc.queryForMap("SELECT status, rejection_reason FROM sporthub.catalog_change_requests WHERE id = ?::uuid", requestId))
                .containsEntry("status", "REJECTED").containsEntry("rejection_reason", "Tên không hợp lệ");
        assertThat(awaitMails(provider.email(), 2)).anySatisfy(m -> {
            assertThat(m.getSubject()).isEqualTo("[Vesanre] Yêu cầu thay đổi San Tu Choi bị từ chối");
            assertThat(m.getText()).contains("Lý do: Tên không hợp lệ");
        });
    }

    @Test
    void shopChangeRequestApproveKeepsSlug() {
        RestClient client = client();
        Admin admin = admin();
        Provider provider = verifiedProvider();
        String slug = jdbc.queryForObject("SELECT slug FROM sporthub.shops WHERE owner_user_id = ?", String.class, provider.userId());
        ResponseEntity<Map> created = post(client, "/api/provider/shop/change-request", Map.of("name", "Shop Ten Moi"), provider.token());
        String requestId = (String) ((Map) created.getBody().get("pendingChange")).get("id");

        assertThat(post(client, "/api/admin/change-requests/" + requestId + "/approve", Map.of(), admin.token())
                .getStatusCode().value()).isEqualTo(200);
        assertThat(jdbc.queryForMap("SELECT name, slug FROM sporthub.shops WHERE owner_user_id = ?", provider.userId()))
                .containsEntry("name", "Shop Ten Moi").containsEntry("slug", slug);
    }

    // Review focus #1: Venue has no @Version, so an unlocked read would write the stale ACTIVE status back.
    @Test
    void approvingAChangeRequestDoesNotUndoAConcurrentSuspend() throws Exception {
        RestClient client = client();
        Admin admin = admin();
        Provider provider = verifiedProvider();
        UUID venueId = pendingVenue(provider.token(), "San Race");
        String requestId = activeVenueWithChange(admin, provider, venueId, Map.of("name", "San Race Moi"));

        try (Connection suspend = dataSource.getConnection()) {
            suspend.setAutoCommit(false);
            try (PreparedStatement ps = suspend.prepareStatement("UPDATE sporthub.venues SET status = 'SUSPENDED' WHERE id = ?")) {
                ps.setObject(1, venueId);
                ps.executeUpdate(); // holds the row lock until commit
            }
            CompletableFuture<ResponseEntity<Map>> approval = CompletableFuture.supplyAsync(() ->
                    post(client, "/api/admin/change-requests/" + requestId + "/approve", Map.of(), admin.token()));
            awaitLockWaiter();
            assertThat(approval).isNotDone();
            suspend.commit();
            assertThat(approval.get(10, TimeUnit.SECONDS).getStatusCode().value()).isEqualTo(200);
        }
        assertThat(jdbc.queryForMap("SELECT status, name FROM sporthub.venues WHERE id = ?", venueId))
                .containsEntry("status", "SUSPENDED").containsEntry("name", "San Race Moi");
    }

    // Venue has no @Version: without @DynamicUpdate the provider's full-row UPDATE would write the stale ACTIVE back.
    @Test
    void providerEditDoesNotUndoAConcurrentSuspend() throws Exception {
        RestClient client = client();
        Admin admin = admin();
        Provider provider = verifiedProvider();
        UUID venueId = pendingVenue(provider.token(), "San Edit Race");
        assertThat(post(client, "/api/admin/venues/" + venueId + "/approve", Map.of(), admin.token()).getStatusCode().value()).isEqualTo(200);

        try (Connection suspend = dataSource.getConnection()) {
            suspend.setAutoCommit(false);
            try (PreparedStatement ps = suspend.prepareStatement("UPDATE sporthub.venues SET status = 'SUSPENDED' WHERE id = ?")) {
                ps.setObject(1, venueId);
                ps.executeUpdate(); // holds the row lock until commit
            }
            CompletableFuture<ResponseEntity<Map>> edit = CompletableFuture.supplyAsync(() ->
                    patch(client, "/api/provider/venues/" + venueId, Map.of("description", "Mo ta moi"), provider.token()));
            awaitLockWaiter();
            assertThat(edit).isNotDone();
            suspend.commit();
            assertThat(edit.get(10, TimeUnit.SECONDS).getStatusCode().value()).isEqualTo(200);
        }
        assertThat(jdbc.queryForMap("SELECT status, description FROM sporthub.venues WHERE id = ?", venueId))
                .containsEntry("status", "SUSPENDED").containsEntry("description", "Mo ta moi");
    }

    @Test
    void cancelDoesNotOverwriteAConcurrentApprove() throws Exception {
        RestClient client = client();
        Admin admin = admin();
        Provider provider = verifiedProvider();
        UUID venueId = pendingVenue(provider.token(), "San Cancel Race");
        String requestId = activeVenueWithChange(admin, provider, venueId, Map.of("name", "San Cancel Moi"));

        try (Connection approve = dataSource.getConnection()) {
            approve.setAutoCommit(false);
            try (PreparedStatement ps = approve.prepareStatement(
                    "SELECT 1 FROM sporthub.catalog_change_requests WHERE id = ?::uuid FOR UPDATE")) {
                ps.setString(1, requestId);
                ps.executeQuery();
            }
            try (PreparedStatement ps = approve.prepareStatement(
                    "UPDATE sporthub.catalog_change_requests SET status = 'APPROVED', reviewed_by = ?, reviewed_at = now() WHERE id = ?::uuid")) {
                ps.setObject(1, admin.userId());
                ps.setString(2, requestId);
                ps.executeUpdate();
            }
            CompletableFuture<ResponseEntity<Map>> cancel = CompletableFuture.supplyAsync(() ->
                    post(client, "/api/provider/change-requests/" + requestId + "/cancel", Map.of(), provider.token()));
            awaitLockWaiter();
            assertThat(cancel).isNotDone();
            approve.commit();
            assertThat(cancel.get(10, TimeUnit.SECONDS).getStatusCode().value()).isEqualTo(409);
        }
        assertThat(jdbc.queryForObject("SELECT status FROM sporthub.catalog_change_requests WHERE id = ?::uuid", String.class, requestId))
                .isEqualTo("APPROVED");
    }

    // Same as the venue case: Shop has no @Version either, so without @DynamicUpdate the provider's full-row UPDATE restores ACTIVE.
    @Test
    void providerShopEditDoesNotUndoAConcurrentSuspend() throws Exception {
        RestClient client = client();
        Provider provider = verifiedProvider(); // shop is ACTIVE

        try (Connection suspend = dataSource.getConnection()) {
            suspend.setAutoCommit(false);
            try (PreparedStatement ps = suspend.prepareStatement("UPDATE sporthub.shops SET status = 'SUSPENDED' WHERE owner_user_id = ?")) {
                ps.setObject(1, provider.userId());
                ps.executeUpdate(); // holds the row lock until commit
            }
            CompletableFuture<ResponseEntity<Map>> edit = CompletableFuture.supplyAsync(() ->
                    patch(client, "/api/provider/shop", Map.of("description", "Mo ta shop moi"), provider.token()));
            awaitLockWaiter();
            assertThat(edit).isNotDone();
            suspend.commit();
            assertThat(edit.get(10, TimeUnit.SECONDS).getStatusCode().value()).isEqualTo(200);
        }
        assertThat(jdbc.queryForMap("SELECT status, description FROM sporthub.shops WHERE owner_user_id = ?", provider.userId()))
                .containsEntry("status", "SUSPENDED").containsEntry("description", "Mo ta shop moi");
    }
}
