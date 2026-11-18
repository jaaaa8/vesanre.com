package com.vesanrebackend.service.payment;

import com.vesanrebackend.entity.coin.CoinTopUp;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Loại test: unit thuần (không Spring) cho FakeTopUpGateway: chữ ký HMAC, parse payload, paymentUrl. */
class FakeTopUpGatewayTest {
    private final FakeTopUpGateway gw = new FakeTopUpGateway("s3cret", "http://lan:8080", new ObjectMapper());
    private final GatewayPayment p = new GatewayPayment("evt_1", "tx_1", "VSRABCDEFGH", 100000L,
            Instant.parse("2026-11-21T10:00:00Z"));

    private HttpHeaders sig(String value) {
        HttpHeaders h = new HttpHeaders();
        h.add("X-Signature", value);
        return h;
    }

    @Test
    void signThenVerify() {
        byte[] body = gw.payload(p);
        assertThat(gw.verify(body, sig(gw.sign(body)))).isTrue();
    }

    @Test
    void uppercaseHexAccepted() {
        byte[] body = gw.payload(p);
        assertThat(gw.verify(body, sig(gw.sign(body).toUpperCase()))).isTrue();
    }

    @Test
    void tamperedBodyRejected() {
        byte[] body = gw.payload(p);
        String signature = gw.sign(body);
        body[body.length - 3] ^= 1;
        assertThat(gw.verify(body, sig(signature))).isFalse();
    }

    @Test
    void missingHeaderRejected() {
        assertThat(gw.verify(gw.payload(p), new HttpHeaders())).isFalse();
    }

    @Test
    void nonHexHeaderRejected() {
        assertThat(gw.verify(gw.payload(p), sig("zz"))).isFalse();
    }

    @Test
    void emptyOrPlaceholderSecretFailsFast() {
        for (String s : new String[]{"", "  ", "replace-with-dev-secret"}) {
            assertThatThrownBy(() -> new FakeTopUpGateway(s, "http://lan:8080", new ObjectMapper()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void parseRoundTrip() {
        assertThat(gw.parse(gw.payload(p))).isEqualTo(p);
    }

    @Test
    void parseMissingFieldIs400() {
        assertThatThrownBy(() -> gw.parse("{\"eventId\":\"e\"}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void parseFractionalAmountIs400() {
        String json = "{\"eventId\":\"e\",\"transactionId\":\"t\",\"transferCode\":\"VSRABCDEFGH\","
                + "\"amountVnd\":100000.5,\"paidAt\":\"2026-11-21T10:00:00Z\"}";
        assertThatThrownBy(() -> gw.parse(json.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void paymentUrlContainsCode() {
        CoinTopUp t = new CoinTopUp();
        t.setTransferCode("VSRABCDEFGH");
        assertThat(gw.paymentUrl(t)).isEqualTo("http://lan:8080/api/payment/fake/VSRABCDEFGH");
    }
}
