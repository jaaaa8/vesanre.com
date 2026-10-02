package com.vesanrebackend.controller.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against real Tomcat + PostgreSQL (SPORTHUB_DB_*), so error dispatch and DB constraints are exercised.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registerLoginProfileAndRoleChecks() {
        RestClient client = client();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "it-" + suffix + "@example.com";
        String phone = "09" + suffix;
        Map<String, Object> customer = Map.of("email", email, "password", "correct-password",
                "displayName", "IT Customer", "phone", phone, "role", "CUSTOMER");

        assertThat(post(client, "/api/auth/register", customer).getStatusCode().value()).isEqualTo(201);

        ResponseEntity<Map> duplicateEmail = post(client, "/api/auth/register", customer);
        assertThat(duplicateEmail.getStatusCode().value()).isEqualTo(409);
        assertThat(duplicateEmail.getBody()).containsEntry("detail", "Email already registered");

        Map<String, Object> samePhone = Map.of("email", "other-" + email, "password", "correct-password",
                "displayName", "IT Other", "phone", phone, "role", "CUSTOMER");
        assertThat(post(client, "/api/auth/register", samePhone).getStatusCode().value()).isEqualTo(409);

        assertThat(post(client, "/api/auth/register", Map.of()).getStatusCode().value()).isEqualTo(400);

        // role is optional and defaults to CUSTOMER.
        Map<String, Object> noRole = Map.of("email", "norole-" + email, "password", "correct-password", "displayName", "IT NoRole");
        assertThat(post(client, "/api/auth/register", noRole).getStatusCode().value()).isEqualTo(201);

        Map<String, Object> asProvider = Map.of("email", "prov-" + email, "password", "correct-password",
                "displayName", "IT Sneaky", "role", "PROVIDER");
        ResponseEntity<Map> sneaky = post(client, "/api/auth/register", asProvider);
        assertThat(sneaky.getStatusCode().value()).isEqualTo(400);
        assertThat(sneaky.getBody()).containsEntry("detail", "Providers must register via /api/auth/register/provider");

        Map<String, Object> longPassword = Map.of("email", "long-" + email, "password", "ư".repeat(40),
                "displayName", "IT Long", "role", "CUSTOMER");
        assertThat(post(client, "/api/auth/register", longPassword).getStatusCode().value()).isEqualTo(400);

        assertThat(post(client, "/api/auth/login", Map.of("email", email, "password", "wrong-password"))
                .getStatusCode().value()).isEqualTo(401);

        ResponseEntity<Map> login = post(client, "/api/auth/login", Map.of("email", email.toUpperCase(), "password", "correct-password"));
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        String token = (String) login.getBody().get("accessToken");

        ResponseEntity<Map> me = get(client, "/api/profile/me", token);
        assertThat(me.getStatusCode().value()).isEqualTo(200);
        assertThat(me.getBody()).containsEntry("email", email);

        assertThat(patch(client, "/api/profile/me", Map.of("phone", "   "), token).getStatusCode().value()).isEqualTo(400);
        assertThat(patch(client, "/api/profile/me", Map.of("phone", ""), token).getStatusCode().value()).isEqualTo(200);

        assertThat(get(client, "/api/profile/me", null).getStatusCode().value()).isEqualTo(401);
        assertThat(get(client, "/api/profile/provider", token).getStatusCode().value()).isEqualTo(403);
        assertThat(get(client, "/api/profile/admin", token).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void providerRegistrationCreatesPendingProfileDraftShopAndVerificationAndBlocksLoginUntilVerified() {
        RestClient client = client();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "provider-" + suffix + "@example.com";
        Map<String, Object> body = Map.of("email", email, "password", "correct-password", "displayName", "IT Provider",
                "legalName", "Công ty Sân Đẹp " + suffix);

        ResponseEntity<Map> registered = post(client, "/api/auth/register/provider", body);
        assertThat(registered.getStatusCode().value()).isEqualTo(201);
        assertThat(registered.getBody()).containsEntry("providerStatus", "PENDING");
        assertThat(post(client, "/api/auth/register/provider", body).getStatusCode().value()).isEqualTo(409);
        assertThat(post(client, "/api/auth/register/provider", Map.of("email", "x-" + email, "password", "correct-password",
                "displayName", "No legal name")).getStatusCode().value()).isEqualTo(400);

        UUID userId = UUID.fromString((String) registered.getBody().get("id"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sporthub.user_roles WHERE user_id = ? AND role_code = 'PROVIDER'",
                Integer.class, userId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM sporthub.provider_profiles WHERE user_id = ?",
                String.class, userId)).isEqualTo("PENDING");
        assertThat(jdbc.queryForMap("SELECT status, slug FROM sporthub.shops WHERE owner_user_id = ?", userId))
                .containsEntry("status", "DRAFT")
                .satisfies(row -> assertThat((String) row.get("slug")).matches("cong-ty-san-dep-" + suffix + "-[a-z0-9]{6}"));
        assertThat(jdbc.queryForMap("SELECT status, submitted_by, documents::text AS documents FROM sporthub.provider_verifications WHERE provider_user_id = ?", userId))
                .containsEntry("status", "PENDING").containsEntry("submitted_by", userId).containsEntry("documents", "[]");

        assertThat(post(client, "/api/auth/login", Map.of("email", email, "password", "wrong-password"))
                .getStatusCode().value()).isEqualTo(401);
        ResponseEntity<Map> pending = post(client, "/api/auth/login", Map.of("email", email, "password", "correct-password"));
        assertThat(pending.getStatusCode().value()).isEqualTo(403);
        assertThat(pending.getBody()).containsEntry("detail", "Provider account is pending approval");

        jdbc.update("UPDATE sporthub.provider_profiles SET status = 'VERIFIED', verified_at = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
        ResponseEntity<Map> login = post(client, "/api/auth/login", Map.of("email", email, "password", "correct-password"));
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        assertThat(get(client, "/api/profile/provider", (String) login.getBody().get("accessToken")).getStatusCode().value()).isEqualTo(200);
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    private ResponseEntity<Map> post(RestClient client, String path, Map<String, Object> body) {
        return client.post().uri(path).body(body).retrieve().toEntity(Map.class);
    }

    private ResponseEntity<Map> patch(RestClient client, String path, Map<String, Object> body, String token) {
        return client.patch().uri(path).headers(headers -> headers.setBearerAuth(token)).body(body).retrieve().toEntity(Map.class);
    }

    private ResponseEntity<Map> get(RestClient client, String path, String token) {
        return client.get().uri(path)
                .headers(headers -> { if (token != null) headers.setBearerAuth(token); })
                .retrieve().toEntity(Map.class);
    }
}
