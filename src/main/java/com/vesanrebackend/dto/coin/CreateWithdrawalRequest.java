package com.vesanrebackend.dto.coin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWithdrawalRequest(@NotNull @Min(100) Long coinAmount,
                                      @NotBlank @Size(max = 120) String bankName,
                                      @NotBlank @Size(max = 40) String bankAccountNumber,
                                      @NotBlank @Size(max = 160) String bankAccountName) {
}
