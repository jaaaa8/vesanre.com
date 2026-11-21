package com.vesanrebackend.dto.coin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarkPaidRequest(@NotBlank @Size(max = 120) String bankTransactionRef) {
}
