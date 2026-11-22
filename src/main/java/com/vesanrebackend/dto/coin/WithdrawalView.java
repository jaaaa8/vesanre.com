package com.vesanrebackend.dto.coin;

import com.vesanrebackend.entity.coin.ShopWithdrawal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WithdrawalView(UUID id, UUID shopId, String shopName, long coinAmount, long feeCoins,
                             BigDecimal payoutVnd, String bankName, String bankAccountNumber, String bankAccountName,
                             String status, String bankTransactionRef, String rejectionReason, Instant reviewedAt,
                             Instant paidAt, Instant createdAt) {
    // Touches the LAZY shop: call inside a transaction.
    public static WithdrawalView of(ShopWithdrawal w) {
        return new WithdrawalView(w.getId(), w.getShop().getId(), w.getShop().getName(), w.getCoinAmount(),
                w.getFeeCoins(), w.getPayoutVnd(), w.getBankName(), w.getBankAccountNumber(), w.getBankAccountName(),
                w.getStatus().name(), w.getBankTransactionRef(), w.getRejectionReason(), w.getReviewedAt(),
                w.getPaidAt(), w.getCreatedAt());
    }
}
