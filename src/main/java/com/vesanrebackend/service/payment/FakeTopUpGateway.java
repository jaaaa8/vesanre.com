package com.vesanrebackend.service.payment;

import com.vesanrebackend.entity.coin.CoinTopUp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Map;

/** Dev-only gateway: HMAC-signed payloads, confirmed from the fake payment page. */
@Component
@ConditionalOnProperty(name = "app.payment.fake.enabled", havingValue = "true")
public class FakeTopUpGateway implements TopUpGateway {
    private final String secret;
    private final String baseUrl;
    private final ObjectMapper json;

    public FakeTopUpGateway(@Value("${app.payment.fake.secret:}") String secret,
                            @Value("${app.payment.fake.base-url:http://localhost:8080}") String baseUrl,
                            ObjectMapper json) {
        if (secret.isBlank() || secret.contains("replace-with")) {
            throw new IllegalStateException("app.payment.fake.secret must be set when the fake gateway is enabled");
        }
        this.secret = secret;
        this.baseUrl = baseUrl;
        this.json = json;
    }

    @Override
    public String code() {
        return "FAKE";
    }

    @Override
    public String paymentUrl(CoinTopUp topUp) {
        return baseUrl + "/api/payment/fake/" + topUp.getTransferCode();
    }

    /** Lowercase hex HMAC-SHA256 of the body. */
    public String sign(byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public byte[] payload(GatewayPayment p) {
        return json.writeValueAsBytes(Map.of("eventId", p.eventId(), "transactionId", p.transactionId(),
                "transferCode", p.transferCode(), "amountVnd", p.amountVnd(), "paidAt", p.paidAt().toString()));
    }

    @Override
    public boolean verify(byte[] rawBody, HttpHeaders headers) {
        String header = headers.getFirst("X-Signature");
        if (header == null) {
            return false;
        }
        try {
            return MessageDigest.isEqual(HexFormat.of().parseHex(header), HexFormat.of().parseHex(sign(rawBody)));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public GatewayPayment parse(byte[] rawBody) {
        try {
            JsonNode n = json.readTree(rawBody);
            JsonNode amount = n.path("amountVnd");
            if (!amount.isIntegralNumber()) {
                throw bad();
            }
            return new GatewayPayment(text(n, "eventId"), text(n, "transactionId"), text(n, "transferCode"),
                    amount.asLong(), Instant.parse(text(n, "paidAt")));
        } catch (JacksonException | DateTimeParseException e) {
            throw bad();
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        if (!v.isString() || v.asString().isBlank()) {
            throw bad();
        }
        return v.asString();
    }

    private static ResponseStatusException bad() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment payload");
    }
}
