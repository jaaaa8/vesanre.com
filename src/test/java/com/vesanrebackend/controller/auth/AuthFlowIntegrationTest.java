package com.vesanrebackend.controller.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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

    @Test
    void registerLoginProfileAndRoleChecks() {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "it-" + suffix + "@example.com";
        String phone = "09" + suffix;
        Map<String, String> customer = Map.of("email", email, "password", "correct-password",
                "displayName", "IT Customer", "phone", phone, "role", "CUSTOMER");

        assertThat(post(client, "/api/auth/register", customer).getStatusCode().value()).isEqualTo(201);

        ResponseEntity<Map> duplicateEmail = post(client, "/api/auth/register", customer);
        assertThat(duplicateEmail.getStatusCode().value()).isEqualTo(409);
        assertThat(duplicateEmail.getBody()).containsEntry("detail", "Email already registered");

        Map<String, String> samePhone = Map.of("email", "other-" + email, "password", "correct-password",
                "displayName", "IT Other", "phone", phone, "role", "CUSTOMER");
        assertThat(post(client, "/api/auth/register", samePhone).getStatusCode().value()).isEqualTo(409);

        assertThat(post(client, "/api/auth/register", Map.of()).getStatusCode().value()).isEqualTo(400);

        Map<String, String> longPassword = Map.of("email", "long-" + email, "password", "ư".repeat(40),
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

        assertThat(get(client, "/api/profile/me", null).getStatusCode().value()).isEqualTo(401);
        assertThat(get(client, "/api/profile/provider", token).getStatusCode().value()).isEqualTo(403);
        assertThat(get(client, "/api/profile/admin", token).getStatusCode().value()).isEqualTo(403);
    }

    private ResponseEntity<Map> post(RestClient client, String path, Map<String, String> body) {
        return client.post().uri(path).body(body).retrieve().toEntity(Map.class);
    }

    private ResponseEntity<Map> get(RestClient client, String path, String token) {
        return client.get().uri(path)
                .headers(headers -> { if (token != null) headers.setBearerAuth(token); })
                .retrieve().toEntity(Map.class);
    }
}
