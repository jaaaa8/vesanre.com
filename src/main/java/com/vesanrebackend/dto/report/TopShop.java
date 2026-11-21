package com.vesanrebackend.dto.report;

import java.util.UUID;

public record TopShop(UUID shopId, String shopName, long bookings) {
}
