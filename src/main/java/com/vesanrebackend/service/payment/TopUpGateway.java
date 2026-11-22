package com.vesanrebackend.service.payment;

import com.vesanrebackend.entity.coin.CoinTopUp;
import org.springframework.http.HttpHeaders;

/** One bean per payment gateway; chosen by {provider} on the webhook URL. */
public interface TopUpGateway {
    String code();

    String paymentUrl(CoinTopUp topUp);

    boolean verify(byte[] rawBody, HttpHeaders headers);

    /** Throws ResponseStatusException(400) on malformed or incomplete payloads. */
    GatewayPayment parse(byte[] rawBody);
}
