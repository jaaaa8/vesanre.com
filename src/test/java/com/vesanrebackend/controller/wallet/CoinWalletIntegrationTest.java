package com.vesanrebackend.controller.wallet;

import com.vesanrebackend.BookingApiTestSupport;
import com.vesanrebackend.entity.enums.CoinEntryType;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Loại test: integration HTTP thật + PostgreSQL (kế thừa BookingApiTestSupport).
 * API: GET /api/wallet, GET /api/wallet/entries; dịch vụ CoinWalletService.
 */
class CoinWalletIntegrationTest extends BookingApiTestSupport {
    // Kiểm tra (Review Focus 5): user chưa có ví -> balance 0, entries rỗng, và GET không tạo dòng ví.
    @Test
    void walletWithoutRowShowsZero() {
        RestClient client = client();
        Customer c = customer();
        ResponseEntity<Map> wallet = get(client, "/api/wallet", c.token());
        assertThat(wallet.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) wallet.getBody().get("balance")).longValue()).isZero();
        ResponseEntity<Map> entries = get(client, "/api/wallet/entries", c.token());
        assertThat(entries.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) entries.getBody().get("items")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sporthub.coin_wallets WHERE user_id = ?", Integer.class,
                c.userId())).isZero();
    }

    // Kiểm tra: nạp hai lần -> số dư = tổng sổ; entries mới nhất trước, balanceAfter đúng, bookingCode null.
    @Test
    @SuppressWarnings("unchecked")
    void topUpCreditsAndListsEntry() {
        Customer c = customer();
        seedCoins(c.userId(), 100);
        seedCoins(c.userId(), 100);
        assertThat(balance(c.userId())).isEqualTo(200).isEqualTo(ledgerSum(c.userId()));

        RestClient client = client();
        assertThat(((Number) get(client, "/api/wallet", c.token()).getBody().get("balance")).longValue()).isEqualTo(200);
        List<Map<String, Object>> items = (List<Map<String, Object>>) get(client, "/api/wallet/entries", c.token())
                .getBody().get("items");
        assertThat(items).hasSize(2);
        assertThat(items).extracting(i -> i.get("type")).containsOnly("TOP_UP");
        assertThat(items).extracting(i -> ((Number) i.get("amount")).longValue()).containsOnly(100L);
        assertThat(items).extracting(i -> ((Number) i.get("balanceAfter")).longValue()).containsExactly(200L, 100L);
        assertThat(items).extracting(i -> i.get("bookingCode")).containsOnlyNulls();
    }

    // Kiểm tra: một top-up không thể cộng coin hai lần (unique top_up_id trên sổ); giao dịch lỗi rollback hết.
    @Test
    void sameTopUpCannotCreditTwice() {
        Customer c = customer();
        UUID topUpId = seedCoins(c.userId(), 100);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> coinWallets.apply(coinWallets.lockUserWallet(c.userId()),
                CoinEntryType.TOP_UP, 50, null, em.getReference(com.vesanrebackend.entity.coin.CoinTopUp.class, topUpId))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(balance(c.userId())).isEqualTo(100);
    }

    // Kiểm tra: không token -> 401.
    @Test
    void walletRequiresLogin() {
        assertThat(get(client(), "/api/wallet", null).getStatusCode().value()).isEqualTo(401);
    }
}
