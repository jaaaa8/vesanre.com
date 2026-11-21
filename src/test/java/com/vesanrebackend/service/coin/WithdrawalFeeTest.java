package com.vesanrebackend.service.coin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Loại test: unit thuần. Phí rút = 20 + ceil(5% X). */
class WithdrawalFeeTest {
    @Test
    void feeIsTwentyPlusFivePercentRoundedUp() {
        assertThat(WithdrawalService.feeCoins(100)).isEqualTo(25);
        assertThat(WithdrawalService.feeCoins(101)).isEqualTo(26); // ceil(5.05)
        assertThat(WithdrawalService.feeCoins(1000)).isEqualTo(70);
    }
}
