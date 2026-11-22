package com.vesanrebackend.dto.coin;

import java.time.Instant;

public record CoinEntryItem(long id, String type, long amount, long balanceAfter, String bookingCode,
                            Instant createdAt) {
}
