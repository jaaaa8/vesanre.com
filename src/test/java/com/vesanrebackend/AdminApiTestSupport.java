package com.vesanrebackend;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lớp nền dùng chung cho mọi integration test HTTP (auth, provider catalog, admin review): khởi động server
 * Tomcat cổng ngẫu nhiên + PostgreSQL thật, JavaMailSender được mock.
 * Cung cấp helper tạo customer/provider/admin, venue chờ duyệt, đăng nhập, gọi GET/POST/PATCH/PUT,
 * đọc các trang danh sách, chờ email mock và chờ row lock. Không chứa test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AdminApiTestSupport {
    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbc;

    @MockitoBean
    protected JavaMailSender mailSender;

    protected record Provider(UUID userId, String email, String token) {
    }

    protected record Admin(UUID userId, String token) {
    }

    /** register -> apply -> JDBC verify + PROVIDER role + ACTIVE shop -> login (roles are read at login). */
    protected Provider verifiedProvider() {
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
        return new Provider(userId, email, login(client, email));
    }

    protected Admin admin() {
        RestClient client = client();
        String email = "admin-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        UUID id = UUID.fromString((String) post(client, "/api/auth/register", Map.of("email", email,
                "password", "correct-password", "displayName", "IT Admin")).getBody().get("id"));
        jdbc.update("INSERT INTO sporthub.user_roles (user_id, role_code) VALUES (?, 'ADMIN')", id); // roles read at login
        return new Admin(id, login(client, email));
    }

    // venue + one court (ACTIVE by default) -> submit; returns the PENDING_REVIEW venue id.
    protected UUID pendingVenue(String token, String name) {
        RestClient client = client();
        String venueId = (String) post(client, "/api/provider/venues", venueBody(name), token).getBody().get("id");
        assertThat(post(client, "/api/provider/venues/" + venueId + "/courts", Map.of("code", "C1", "name", "Court 1",
                "capacity", 4, "bookingStepMinutes", 30, "minBookingMinutes", 60, "maxBookingMinutes", 120), token)
                .getStatusCode().value()).isEqualTo(201);
        assertThat(post(client, "/api/provider/venues/" + venueId + "/submit", Map.of(), token).getStatusCode().value())
                .isEqualTo(200);
        return UUID.fromString(venueId);
    }

    // The shared test DB keeps queues growing across runs: walk every page instead of assuming page 0.
    protected List<Map<String, Object>> allItems(RestClient client, String path, String token) {
        List<Map<String, Object>> items = new ArrayList<>();
        String sep = path.contains("?") ? "&" : "?";
        for (int page = 0; ; page++) {
            ResponseEntity<Map> response = get(client, path + sep + "size=100&page=" + page, token);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            items.addAll((List<Map<String, Object>>) response.getBody().get("items"));
            if (page + 1 >= ((Number) response.getBody().get("totalPages")).intValue()) {
                return items;
            }
        }
    }

    protected List<SimpleMailMessage> mailsTo(String email) {
        return Mockito.mockingDetails(mailSender).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("send") && i.getArgument(0) instanceof SimpleMailMessage)
                .map(i -> (SimpleMailMessage) i.getArgument(0))
                .filter(m -> m.getTo() != null && Arrays.asList(m.getTo()).contains(email))
                .toList();
    }

    // Mails go out @Async after commit: poll briefly.
    protected List<SimpleMailMessage> awaitMails(String email, int count) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (mailsTo(email).size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(mailsTo(email)).hasSize(count);
        return mailsTo(email);
    }

    // Proves a request is blocked on a row lock (instead of sleeping and hoping): some backend is waiting on a Lock.
    protected void awaitLockWaiter() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND datname = current_database()",
                    Integer.class) > 0) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("no backend is waiting on a row lock");
    }

    protected Map<String, Object> venueBody(String name) {
        return Map.of("name", name, "addressLine", "1 Le Loi", "district", "Quan 1", "city", "HCM",
                "latitude", 10.5, "longitude", 106.7);
    }

    protected RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    protected String login(RestClient client, String email) {
        return (String) post(client, "/api/auth/login", Map.of("email", email, "password", "correct-password"))
                .getBody().get("accessToken");
    }

    protected ResponseEntity<Map> get(RestClient client, String path, String token) {
        return client.get().uri(path)
                .headers(headers -> { if (token != null) headers.setBearerAuth(token); })
                .retrieve().toEntity(Map.class);
    }

    protected ResponseEntity<Map> post(RestClient client, String path, Map<String, Object> body) {
        return post(client, path, body, null);
    }

    protected ResponseEntity<Map> post(RestClient client, String path, Map<String, Object> body, String token) {
        return client.post().uri(path).headers(headers -> { if (token != null) headers.setBearerAuth(token); })
                .body(body).retrieve().toEntity(Map.class);
    }

    protected ResponseEntity<Map> patch(RestClient client, String path, Map<String, Object> body, String token) {
        return client.patch().uri(path).headers(headers -> headers.setBearerAuth(token)).body(body).retrieve().toEntity(Map.class);
    }

    protected ResponseEntity<Map> put(RestClient client, String path, Object body, String token) {
        return client.put().uri(path).headers(headers -> headers.setBearerAuth(token)).body(body).retrieve().toEntity(Map.class);
    }
}
