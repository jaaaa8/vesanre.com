package com.vesanrebackend.service.payment;

import java.time.Instant;

/** A paid-transfer notification, normalized across gateways. */
public record GatewayPayment(String eventId, String transactionId, String transferCode, long amountVnd, Instant paidAt) {
}
