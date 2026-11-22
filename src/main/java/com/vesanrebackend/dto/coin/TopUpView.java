package com.vesanrebackend.dto.coin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TopUpView(UUID id, String transferCode, long coins, BigDecimal amountVnd, String status,
                        Instant expiresAt, Instant paidAt, String paymentUrl) {
}
