package com.vesanrebackend.controller.admin;

import com.vesanrebackend.BookingApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUserIntegrationTest extends BookingApiTestSupport {

    private int loginStatus(String email) {
        return post(client(), "/api/auth/login", Map.of("email", email, "password", "correct-password"))
                .getStatusCode().value();
    }

    private int suspend(UUID id, Map<String, Object> body, String token) {
        return post(client(), "/api/admin/users/" + id + "/suspend", body, token).getStatusCode().value();
    }

    private int reactivate(UUID id, String token) {
        return post(client(), "/api/admin/users/" + id + "/reactivate", Map.of(), token).getStatusCode().value();
    }

    // Template variables make RestClient encode % and _ as query values.
    private ResponseEntity<Map> list(String query, String token, Object... vars) {
        return client().get().uri("/api/admin/users?" + query, vars)
                .headers(h -> h.setBearerAuth(token)).retrieve().toEntity(Map.class);
    }

    @Test
    void suspendBlocksLoginAndReactivateRestores() {
        Admin a = admin();
        Customer c = customer();
        UUID id = c.userId();
        assertThat(suspend(id, Map.of("reason", "spam"), a.token())).isEqualTo(200);
        assertThat(loginStatus(c.email())).isEqualTo(401);
        assertThat(suspend(id, Map.of("reason", "x"), a.token())).isEqualTo(409);
        assertThat(reactivate(id, a.token())).isEqualTo(200);
        assertThat(loginStatus(c.email())).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sporthub.audit_logs WHERE entity_id = ?", Long.class, id))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT after_data->>'reason' FROM sporthub.audit_logs "
                + "WHERE entity_id = ? AND action = 'USER_SUSPENDED'", String.class, id)).isEqualTo("spam");
    }

    @Test
    void cannotSuspendSelfOrAdmin() {
        Admin a = admin();
        Admin other = admin();
        assertThat(suspend(a.userId(), Map.of("reason", "x"), a.token())).isEqualTo(409);
        assertThat(suspend(other.userId(), Map.of("reason", "x"), a.token())).isEqualTo(409);
        assertThat(suspend(UUID.randomUUID(), Map.of("reason", "x"), a.token())).isEqualTo(404);
    }

    @Test
    void reactivateOnlyFromSuspended() {
        Admin a = admin();
        Customer c = customer();
        assertThat(reactivate(c.userId(), a.token())).isEqualTo(409);
        jdbc.update("UPDATE sporthub.users SET status = 'DEACTIVATED' WHERE id = ?", c.userId());
        assertThat(reactivate(c.userId(), a.token())).isEqualTo(409);
        assertThat(suspend(c.userId(), Map.of("reason", "x"), a.token())).isEqualTo(409);
    }

    @Test
    void suspendRequiresReason() {
        Admin a = admin();
        Customer c = customer();
        assertThat(suspend(c.userId(), Map.of("reason", ""), a.token())).isEqualTo(400);
        assertThat(suspend(c.userId(), Map.of(), a.token())).isEqualTo(400);
        assertThat(suspend(c.userId(), Map.of("reason", "x".repeat(501)), a.token())).isEqualTo(400);
        assertThat(suspend(c.userId(), Map.of("reason", "x".repeat(500)), a.token())).isEqualTo(200);
    }

    @Test
    void listFiltersByQRoleStatus() {
        Admin a = admin();
        Customer c = customer();
        String rand = c.email().substring("customer-".length(), "customer-".length() + 8);
        Map body = list("q={q}", a.token(), rand.toUpperCase()).getBody();
        List<Map> items = (List<Map>) body.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("id")).isEqualTo(c.userId().toString());
        assertThat((List<String>) items.get(0).get("roles")).contains("CUSTOMER");
        assertThat(items.get(0).get("status")).isEqualTo("ACTIVE");
        assertThat(list("q={q}&status=SUSPENDED", a.token(), rand).getBody().get("totalElements")).isEqualTo(0);
        assertThat(list("q={q}&role=ADMIN", a.token(), rand).getBody().get("totalElements")).isEqualTo(0);
        assertThat(list("q={q}&role=CUSTOMER&status=ACTIVE", a.token(), rand).getBody().get("totalElements")).isEqualTo(1);
        long all = ((Number) list("size=1", a.token()).getBody().get("totalElements")).longValue();
        ResponseEntity<Map> percent = list("q={q}", a.token(), "%");
        assertThat(percent.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) percent.getBody().get("totalElements")).longValue()).isLessThan(all);
        ResponseEntity<Map> underscore = list("q={q}", a.token(), "_");
        assertThat(underscore.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) underscore.getBody().get("totalElements")).longValue()).isLessThan(all);
        assertThat(list("status=BOGUS", a.token()).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void nonAdminForbidden() {
        Customer c = customer();
        assertThat(get(client(), "/api/admin/users", c.token()).getStatusCode().value()).isEqualTo(403);
        assertThat(suspend(c.userId(), Map.of("reason", "x"), c.token())).isEqualTo(403);
    }
}
