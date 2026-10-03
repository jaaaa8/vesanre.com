package com.vesanrebackend.controller.auth;

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
                "displayName", "IT Customer", "phone", phone);

        assertThat(post(client, "/api/auth/register", customer).getStatusCode().value()).isEqualTo(201);

        ResponseEntity<Map> duplicateEmail = post(client, "/api/auth/register", customer);
        assertThat(duplicateEmail.getStatusCode().value()).isEqualTo(409);
        assertThat(duplicateEmail.getBody()).containsEntry("detail", "Email already registered");

        Map<String, Object> samePhone = Map.of("email", "other-" + email, "password", "correct-password",
                "displayName", "IT Other", "phone", phone);
        assertThat(post(client, "/api/auth/register", samePhone).getStatusCode().value()).isEqualTo(409);

        assertThat(post(client, "/api/auth/register", Map.of()).getStatusCode().value()).isEqualTo(400);

        Map<String, Object> noRole = Map.of("email", "norole-" + email, "password", "correct-password", "displayName", "IT NoRole");
        assertThat(post(client, "/api/auth/register", noRole).getStatusCode().value()).isEqualTo(201);

        // A legacy role field is ignored: registration always creates a CUSTOMER.
        Map<String, Object> asProvider = Map.of("email", "prov-" + email, "password", "correct-password",
                "displayName", "IT Sneaky", "role", "PROVIDER");
        ResponseEntity<Map> sneaky = post(client, "/api/auth/register", asProvider);
        assertThat(sneaky.getStatusCode().value()).isEqualTo(201);
        assertThat(jdbc.queryForList("SELECT role_code FROM sporthub.user_roles WHERE user_id = ?", String.class,
                UUID.fromString((String) sneaky.getBody().get("id")))).containsExactly("CUSTOMER");

        Map<String, Object> longPassword = Map.of("email", "long-" + email, "password", "ư".repeat(40),
                "displayName", "IT Long");
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
    void providerApplicationCreatesPendingProfileDraftShopAndVerificationWhileLoginStaysOpen() {
        RestClient client = client();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "provider-" + suffix + "@example.com";
        UUID userId = UUID.fromString((String) post(client, "/api/auth/register", Map.of("email", email,
                "password", "correct-password", "displayName", "IT Provider")).getBody().get("id"));
        String token = login(client, email);
        Map<String, Object> body = Map.of("legalName", "C\u00f4ng ty S\u00e2n \u0110\u1eb9p " + suffix);

        assertThat(post(client, "/api/profile/provider-application", body).getStatusCode().value()).isEqualTo(401);
        assertThat(postAs(client, "/api/profile/provider-application", Map.of(), token).getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map> applied = postAs(client, "/api/profile/provider-application", body, token);
        assertThat(applied.getStatusCode().value()).isEqualTo(201);
        assertThat(applied.getBody()).containsEntry("providerStatus", "PENDING");
        assertThat(postAs(client, "/api/profile/provider-application", body, token).getStatusCode().value()).isEqualTo(409);

        assertThat(jdbc.queryForList("SELECT role_code FROM sporthub.user_roles WHERE user_id = ?", String.class, userId))
                .containsExactly("CUSTOMER");
        assertThat(jdbc.queryForObject("SELECT status FROM sporthub.provider_profiles WHERE user_id = ?",
                String.class, userId)).isEqualTo("PENDING");
        assertThat(jdbc.queryForMap("SELECT status, slug FROM sporthub.shops WHERE owner_user_id = ?", userId))
                .containsEntry("status", "DRAFT")
                .satisfies(row -> assertThat((String) row.get("slug")).matches("cong-ty-san-dep-" + suffix + "-[a-z0-9]{6}"));
        assertThat(jdbc.queryForMap("SELECT status, submitted_by, documents::text AS documents FROM sporthub.provider_verifications WHERE provider_user_id = ?", userId))
                .containsEntry("status", "PENDING").containsEntry("submitted_by", userId).containsEntry("documents", "[]");

        ResponseEntity<Map> pending = post(client, "/api/auth/login", Map.of("email", email, "password", "correct-password"));
        assertThat(pending.getStatusCode().value()).isEqualTo(200);
        assertThat((Map<String, Object>) pending.getBody().get("user")).containsEntry("providerStatus", "PENDING");
        assertThat(get(client, "/api/profile/provider", (String) pending.getBody().get("accessToken")).getStatusCode().value()).isEqualTo(403);

        jdbc.update("UPDATE sporthub.provider_profiles SET status = 'VERIFIED', verified_at = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
        assertThat(postAs(client, "/api/profile/provider-application", body, token).getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void adminApprovalGrantsProviderRoleAndRejectedApplicantCanReapply() {
        RestClient client = client();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminEmail = "admin-" + suffix + "@example.com";
        UUID adminId = UUID.fromString((String) post(client, "/api/auth/register", Map.of("email", adminEmail,
                "password", "correct-password", "displayName", "IT Admin")).getBody().get("id"));
        // Roles are read at login, so elevate first and log in afterwards.
        jdbc.update("INSERT INTO sporthub.user_roles (user_id, role_code) VALUES (?, 'ADMIN')", adminId);
        String adminToken = (String) post(client, "/api/auth/login", Map.of("email", adminEmail, "password", "correct-password"))
                .getBody().get("accessToken");

        String approveEmail = "approve-" + suffix + "@example.com";
        String rejectEmail = "reject-" + suffix + "@example.com";
        UUID approveId = registerProvider(client, approveEmail, "Approve " + suffix);
        UUID rejectId = registerProvider(client, rejectEmail, "Reject " + suffix);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sporthub.user_roles WHERE user_id = ? AND role_code = 'PROVIDER'",
                Integer.class, approveId)).isZero();

        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (int page = 0; ; page++) {
            ResponseEntity<Map> response = client.get().uri("/api/admin/providers?size=100&page=" + page)
                    .headers(headers -> headers.setBearerAuth(adminToken)).retrieve().toEntity(Map.class);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            items.addAll((List<Map<String, Object>>) response.getBody().get("items"));
            if (page + 1 >= ((Number) response.getBody().get("totalPages")).intValue()) break;
        }
        assertThat(items).extracting(item -> item.get("userId")).contains(approveId.toString(), rejectId.toString());
        Map<String, Object> item = items.stream().filter(i -> approveId.toString().equals(i.get("userId"))).findFirst().orElseThrow();
        assertThat(item).containsEntry("email", approveEmail).containsEntry("status", "PENDING");
        assertThat((Map<String, Object>) item.get("shop")).containsEntry("status", "DRAFT");
        assertThat((Map<String, Object>) item.get("verification")).containsEntry("status", "PENDING");

        ResponseEntity<Map> approved = postAs(client, "/api/admin/providers/" + approveId + "/approve", null, adminToken);
        assertThat(approved.getStatusCode().value()).isEqualTo(200);
        assertThat(approved.getBody()).containsEntry("status", "VERIFIED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sporthub.user_roles WHERE user_id = ? AND role_code = 'PROVIDER'",
                Integer.class, approveId)).isEqualTo(1);
        assertThat(postAs(client, "/api/admin/providers/" + approveId + "/approve", null, adminToken).getStatusCode().value()).isEqualTo(409);
        assertThat(postAs(client, "/api/admin/providers/" + UUID.randomUUID() + "/approve", null, adminToken).getStatusCode().value()).isEqualTo(404);

        ResponseEntity<Map> providerLogin = post(client, "/api/auth/login", Map.of("email", approveEmail, "password", "correct-password"));
        assertThat(providerLogin.getStatusCode().value()).isEqualTo(200);
        String providerToken = (String) providerLogin.getBody().get("accessToken");
        assertThat(postAs(client, "/api/admin/providers/" + rejectId + "/approve", null, providerToken).getStatusCode().value()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT status FROM sporthub.shops WHERE owner_user_id = ?", String.class, approveId)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForMap("SELECT status, reviewed_by, reviewed_at IS NOT NULL AS reviewed FROM sporthub.provider_verifications WHERE provider_user_id = ?", approveId))
                .containsEntry("status", "APPROVED").containsEntry("reviewed_by", adminId).containsEntry("reviewed", true);
        assertThat(jdbc.queryForMap("SELECT actor_user_id, before_data->>'status' AS before, after_data->>'status' AS after FROM sporthub.audit_logs WHERE entity_id = ? AND action = 'PROVIDER_APPROVED'", approveId))
                .containsEntry("actor_user_id", adminId).containsEntry("before", "PENDING").containsEntry("after", "VERIFIED");

        assertThat(postAs(client, "/api/admin/providers/" + rejectId + "/reject", Map.of("reason", " "), adminToken).getStatusCode().value()).isEqualTo(400);
        ResponseEntity<Map> rejected = postAs(client, "/api/admin/providers/" + rejectId + "/reject", Map.of("reason", "Invalid tax id"), adminToken);
        assertThat(rejected.getStatusCode().value()).isEqualTo(200);
        assertThat(rejected.getBody()).containsEntry("status", "REJECTED");
        ResponseEntity<Map> rejectedLogin = post(client, "/api/auth/login", Map.of("email", rejectEmail, "password", "correct-password"));
        assertThat(rejectedLogin.getStatusCode().value()).isEqualTo(200);
        assertThat((Map<String, Object>) rejectedLogin.getBody().get("user")).containsEntry("providerStatus", "REJECTED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sporthub.user_roles WHERE user_id = ? AND role_code = 'PROVIDER'",
                Integer.class, rejectId)).isZero();
        assertThat(jdbc.queryForMap("SELECT status, rejection_reason, reviewed_by FROM sporthub.provider_verifications WHERE provider_user_id = ?", rejectId))
                .containsEntry("status", "REJECTED").containsEntry("rejection_reason", "Invalid tax id").containsEntry("reviewed_by", adminId);
        assertThat(jdbc.queryForObject("SELECT after_data->>'reason' FROM sporthub.audit_logs WHERE entity_id = ? AND action = 'PROVIDER_REJECTED'",
                String.class, rejectId)).isEqualTo("Invalid tax id");
        assertThat(jdbc.queryForObject("SELECT status FROM sporthub.shops WHERE owner_user_id = ?", String.class, rejectId)).isEqualTo("DRAFT");

        String slug = jdbc.queryForObject("SELECT slug FROM sporthub.shops WHERE owner_user_id = ?", String.class, rejectId);
        ResponseEntity<Map> reapplied = postAs(client, "/api/profile/provider-application",
                Map.of("legalName", "Reject again " + suffix), (String) rejectedLogin.getBody().get("accessToken"));
        assertThat(reapplied.getStatusCode().value()).isEqualTo(201);
        assertThat(reapplied.getBody()).containsEntry("providerStatus", "PENDING");
        assertThat(jdbc.queryForObject("SELECT status FROM sporthub.provider_profiles WHERE user_id = ?", String.class, rejectId)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT slug FROM sporthub.shops WHERE owner_user_id = ?", String.class, rejectId)).isEqualTo(slug);
        assertThat(jdbc.queryForList("SELECT status FROM sporthub.provider_verifications WHERE provider_user_id = ? ORDER BY created_at", String.class, rejectId))
                .containsExactly("REJECTED", "PENDING");
    }

    private UUID registerProvider(RestClient client, String email, String legalName) {
        UUID userId = UUID.fromString((String) post(client, "/api/auth/register", Map.of("email", email,
                "password", "correct-password", "displayName", "IT Provider")).getBody().get("id"));
        ResponseEntity<Map> applied = postAs(client, "/api/profile/provider-application", Map.of("legalName", legalName), login(client, email));
        assertThat(applied.getStatusCode().value()).isEqualTo(201);
        return userId;
    }

    private String login(RestClient client, String email) {
        return (String) post(client, "/api/auth/login", Map.of("email", email, "password", "correct-password"))
                .getBody().get("accessToken");
    }

    private ResponseEntity<Map> postAs(RestClient client, String path, Map<String, Object> body, String token) {
        RestClient.RequestBodySpec spec = client.post().uri(path).headers(headers -> headers.setBearerAuth(token));
        return (body == null ? spec : spec.body(body)).retrieve().toEntity(Map.class);
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
