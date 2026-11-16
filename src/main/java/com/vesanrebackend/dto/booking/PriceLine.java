package com.vesanrebackend.dto.booking;

import java.math.BigDecimal;
import java.time.LocalTime;

public record PriceLine(LocalTime start, LocalTime end, BigDecimal price) {
}
