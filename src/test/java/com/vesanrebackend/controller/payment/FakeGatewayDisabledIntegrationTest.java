package com.vesanrebackend.controller.payment;

import com.vesanrebackend.BookingApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loại test: integration HTTP thật, cổng FAKE tắt (mặc định production).
 * API: GET /api/payment/fake/{code}, POST /api/payment/webhooks/FAKE, POST /api/wallet/top-ups.
 */
@TestPropertySource(properties = "app.payment.fake.enabled=false")
class FakeGatewayDisabledIntegrationTest extends BookingApiTestSupport {
    // Kiểm tra: fake tắt -> trang 404, webhook FAKE 404, tạo lệnh nạp 503.
    @Test
    void everythingOff() {
        RestClient client = client();
        assertThat(client.get().uri("/api/payment/fake/VSRABCDEFGH").retrieve().toEntity(String.class)
                .getStatusCode().value()).isEqualTo(404);
        assertThat(client.post().uri("/api/payment/webhooks/FAKE").contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", "00").body("{}".getBytes()).retrieve().toEntity(String.class)
                .getStatusCode().value()).isEqualTo(404);
        Customer c = customer();
        assertThat(post(client, "/api/wallet/top-ups", Map.of("coins", 100), c.token()).getStatusCode().value())
                .isEqualTo(503);
        assertThat(post(client, "/api/wallet/top-ups/" + UUID.randomUUID() + "/simulate-paid", Map.of(), c.token())
                .getStatusCode().value()).isEqualTo(404);
    }
}
