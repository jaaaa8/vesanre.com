package com.vesanrebackend.dto.report;

import java.math.BigDecimal;

/** VND sums are for the month (VN time); coin totals are current balances. */
public record FinanceReport(String month, BigDecimal customerTopUpVnd, BigDecimal providerFeeVnd,
                            BigDecimal providerPayoutVnd, long shopWalletCoins, long pendingWithdrawalCoins) {
}
